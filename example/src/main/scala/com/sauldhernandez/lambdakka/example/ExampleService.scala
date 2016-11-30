package com.sauldhernandez.lambdakka.example

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.sauldhernandez.lambdakka.core.HttpLambdaExecutor
import java.io.{FileInputStream, FileOutputStream, InputStream, OutputStream}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by saul on 11/26/16.
  */
class ExampleService extends HttpLambdaExecutor {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()


  //Our sample route
  val route : Route = {
    import akka.http.scaladsl.server.Directives._
    (get & path("hello")) {
      complete("Hello world!!")
    }
  }

  //Create the re-usable portion of the graph for the function
  private val routeFlow = route.join(lambdaLayer)

  //Our function
  def service(input : InputStream, output : OutputStream) : Unit = {

    val waitable = runFlow(routeFlow, input, output)

    //Not sure about this one... will try without it
    Await.result(waitable, 5 seconds)
    input.close()
    output.close()
  }

  def test() : Unit = {
    val input = getClass.getClassLoader.getResourceAsStream("test.json")
    val output = new FileOutputStream("output.json")

    service(input, output)
    input.close()
    output.close()
  }

  def terminate() : Unit = {
    system.terminate()
  }
}

