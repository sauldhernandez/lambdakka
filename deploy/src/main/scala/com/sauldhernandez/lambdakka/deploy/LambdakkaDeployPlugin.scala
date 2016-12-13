package com.sauldhernandez.lambdakka.deploy

import java.io.{FileInputStream, InputStream}
import java.nio.file.Files

import sbt._
import sbt.plugins.JvmPlugin
import sbtassembly.AssemblyPlugin
import Keys._
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.CreateStackRequest
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.amazonaws.util.IOUtils
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils

import scala.collection.JavaConverters._

/**
  * Created by saul on 11/29/16.
  */
object LambdakkaDeployPlugin extends AutoPlugin {

  override def requires: Plugins = AssemblyPlugin && JvmPlugin

  object autoImport {
    lazy val s3BucketName = SettingKey[String]("s3-bucket-name", "The name of the s3 bucket were lambda code will be uploaded")
    lazy val cloudFormationSourceDirectory = SettingKey[File]("cloudformation-source")
    lazy val cloudFormationFile = SettingKey[File]("cloudformation-file")
    lazy val runCloudformation = TaskKey[Unit]("run-cloudformation")
  }

  import autoImport._

  override def projectSettings = Seq(
    s3BucketName := "lambda-" + name.value,
    cloudFormationSourceDirectory := sourceDirectory.value / "cloudformation",
    cloudFormationFile := cloudFormationSourceDirectory.value / "main.yml",
    runCloudformation := execCloudFormation(
      streams.value.log,
      cloudFormationSourceDirectory.value,
      s3BucketName.value,
      stackName = name.value,
      cloudFormationTemplate = ""
    )
  )

  private def uploadCloudFormationData(bucketName : String, files : Seq[(String, InputStream, Long)]) = {
    val s3 = new AmazonS3Client()

    s3.createBucket(bucketName)

    //Delete everything in place
    val currentObjects = s3.listObjects(bucketName).getObjectSummaries.asScala
    currentObjects.foreach( obj => s3.deleteObject(bucketName, obj.getKey))

    files.foreach {
      case (filename, contents, length) =>
        val metadata = new ObjectMetadata()
        metadata.setContentLength(length)
        s3.putObject(new PutObjectRequest(bucketName, filename, contents, metadata))
    }
  }

  private def execCloudFormation(
                                  logger : Logger,
                                  cloudFormationDirectory : File,
                                  bucketName : String,
                                  stackName : String,
                                  cloudFormationTemplate : String) = {

    //TODO: Merge cloudFormationTemplate with existing file before upload

    logger.info("Uploading files from " + cloudFormationDirectory.toPath.toAbsolutePath.toString)
    val files = (cloudFormationDirectory ** "*.*").get.map { f =>
      val path = cloudFormationDirectory.toPath.relativize(f.toPath).toString
      val stream = new FileInputStream(f.toPath.toFile)
      logger.info(path)
      (path, stream, f.getAbsoluteFile.length())
    }

    uploadCloudFormationData(bucketName, files)


    //Get all files in cloudformation folder.

//    val client = new AmazonCloudFormationClient()
//
//    val request = new CreateStackRequest()
//      .withStackName(stackName)
//      .withTemplateBody(cloudFormationTemplate)
//
//    client.createStack(request)
  }

  private def sha1(contents : Array[Byte]) : String = {
    val digest = DigestUtils.getSha1Digest
    val output = digest.digest(contents)
    Base64.encodeBase64String(output)
  }
}
