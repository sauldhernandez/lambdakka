package com.sauldhernandez.lambdakka.core

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets

import akka.NotUsed
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl.BidiFlow
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, StreamConverters}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import spray.json._

import scala.concurrent.Future

/**
  * Runs an akka http route as an AWS Lambda function
  */
trait HttpLambdaExecutor {

  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)
  private val bodyCharset = StandardCharsets.UTF_8

  type LambdaLayer = BidiFlow[HttpResponse, ByteString, ByteString, HttpRequest, NotUsed]

  private def parseHttpMethod(method : String) = method match {
    case "CONNECT" => HttpMethods.CONNECT
    case "DELETE" => HttpMethods.DELETE
    case "GET" => HttpMethods.GET
    case "HEAD" => HttpMethods.HEAD
    case "OPTIONS" => HttpMethods.OPTIONS
    case "PATCH" => HttpMethods.PATCH
    case "POST" => HttpMethods.POST
    case "PUT" => HttpMethods.PUT
    case "TRACE" => HttpMethods.TRACE
    case x => HttpMethod.custom(x)
  }

  private def parseHeader(name : String, value : String) : HttpHeader = HttpHeader.parse(name, value) match {
    case ParsingResult.Ok(header, _) => header
    case ParsingResult.Error(error) => throw new IllegalArgumentException(s"Could not parse http header. ${error.formatPretty}")
  }

  private def httpRequestFromAPIGatewayJson(json : JsValue) : HttpRequest = {

    val inputObject = json.asJsObject

    val method = inputObject.fields.get("httpMethod") match {
      case Some(JsString(methodValue)) => parseHttpMethod(methodValue)
      case _ => throw new IllegalArgumentException("httpMethod must be a string")
    }

    val headers = inputObject.fields.get("headers") match {
      case Some(JsObject(headerValues)) => headerValues.toSeq.map {
        case (k, v : JsString) => parseHeader(k, v.value)
        case _ => throw new IllegalArgumentException("http headers must have string value")
      }
      case Some(JsNull) | None => Seq()
      case _ => throw new IllegalArgumentException("headers must be an object")
    }

    //Get the X-Forwarded-Proto header value. Default to "http" is not found
    val scheme = headers.find(_.lowercaseName() == "x-forwarded-proto").map(_.value()).getOrElse("http")
    //Get the X-Forwarded-Port header value. Default to 80 and 443 for http and https respectively.
    val port = headers.find(_.lowercaseName() == "x-forwarded-port").map(_.value().toInt).getOrElse(scheme match {
      case "http" => 80
      case "https" => 443
    })

    val host = headers.find(_.lowercaseName() == "host").map(_.value()).getOrElse("localhost")

    val uri = Uri.from(
      scheme = scheme,
      host = host,
      port = port,
      path = inputObject.fields.get("path") match {
        case Some(JsString(value)) => value
        case _ => throw new IllegalArgumentException("path must be a string")
      }
    ).withQuery(Uri.Query(inputObject.fields.get("queryStringParameters") match {
      case Some(JsObject(queryValues)) => queryValues.mapValues {
        case JsString(v) => v
        case _ => throw new IllegalArgumentException("query string values must be string")
      }
      case Some(JsNull) => Map[String, String]()
      case _ => throw new IllegalArgumentException("queryStringParameters must be an object")
    }))


    //Get the Content-Type header
    val maybeContentType = headers.find(_.lowercaseName() == "content-type").map(_.value())
    val bodyEntity = inputObject.fields.get("body") match {
      case Some(JsString(value)) => maybeContentType match {
        case Some(ctValue) => ContentType.parse(ctValue).fold(
            err => throw new IllegalArgumentException("Could not parse content type: " + err.foldLeft("")( (prev, err) => prev + err.formatPretty)),
            contentType => HttpEntity(contentType, value.getBytes(bodyCharset))
          )
        case None => HttpEntity(value.getBytes(bodyCharset))
      }
      case Some(JsNull) => HttpEntity.Empty
      case None => HttpEntity.Empty
      case Some(_) => throw new IllegalArgumentException("body must be a string")
    }

    HttpRequest(
      method = method,
      headers = headers.toVector,
      uri = uri,
      entity = bodyEntity
    )
  }

  private class Response2Json extends GraphStage[FlowShape[HttpResponse, Future[JsValue]]] {
    private val in = Inlet[HttpResponse]("Response2Json.in")
    private val out = Outlet[Future[JsValue]]("Response2Json.out")

    override val shape = FlowShape(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with InHandler with OutHandler {

      setHandlers(in, out, this)

      override def onPush(): Unit = {
        val response = grab(in)
        //Run the inner graph
        val dataGraph = response.entity.dataBytes.toMat(Sink.fold(ByteString.empty)( (a, b) => a ++ b))(Keep.right)
        val dataResult = dataGraph.run()(materializer)

        emit(out, dataResult.map { entityData =>
          JsObject(
            "statusCode" -> JsNumber(response.status.intValue()),
            "headers" -> JsObject(response.headers.map(h => (h.name(), JsString(h.value()))).toMap + ("Content-Type" -> JsString(response.entity.contentType.value))),
            "body" -> JsString(entityData.decodeString(bodyCharset))
          )
        } (materializer.executionContext))
      }

      override def onPull(): Unit = pull(in)

      override def onDownstreamFinish(): Unit = cancel(in)
    }
  }

  lazy val lambdaLayer : LambdaLayer = {

    val requestParser = Flow.fromFunction(httpRequestFromAPIGatewayJson)
    val responseParser = new Response2Json()
    val jsonPrinter = Flow.fromFunction { js : JsValue => ByteString(js.compactPrint) }


    val in = Flow.fromGraph(
      Flow[ByteString]
        .fold(ByteString.empty)((a, b) => a ++ b)
        .map(bs => JsonParser(ParserInput(bs.decodeString(bodyCharset))))
        .via(requestParser)
    )

    val out = Flow.fromGraph(
      Flow[HttpResponse]
        .via(responseParser).mapAsync(2)(identity)
        .recover {
          case e : IllegalArgumentException =>
            logger.error("Error while processing request", e)
            JsObject(
              "statusCode" -> JsNumber(500),
              "headers" -> JsObject("Content-Type" -> JsString("text/plain")),
              "body" -> JsString("Lambda parse failed")
            )
        }
        .via(jsonPrinter)
    )

    BidiFlow.fromFlows(out, in)
  }

  def runFlow(lambdaFlow : Flow[ByteString, ByteString, Any], input : InputStream, output : OutputStream, chunkSize : Int = 1024)(implicit materializer: ActorMaterializer) : Future[IOResult] = {

    //Define the graph pieces
    val byteSource = StreamConverters.fromInputStream({ () => input }, chunkSize)
    val byteSink = StreamConverters.fromOutputStream({ () => output }, true)

    val graph = RunnableGraph.fromGraph(
        byteSource
          .via(lambdaFlow)
          .toMat(byteSink)(Keep.right)
    )

    graph.run()
  }

}
