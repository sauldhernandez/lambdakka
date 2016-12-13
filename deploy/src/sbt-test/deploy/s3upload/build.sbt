import java.io.FileInputStream

import com.amazonaws.services.s3.AmazonS3Client
import org.apache.commons.io.IOUtils
import scala.collection.JavaConverters._

enablePlugins(LambdakkaDeployPlugin)

name := "deploy-test-s3"

TaskKey[Unit]("check") := {
  //Check that all files are there
  val logger = streams.value.log
  val s3 = new AmazonS3Client()

  val files = (cloudFormationSourceDirectory.value ** "*.*").get

  files.foreach { f =>
    val path = cloudFormationSourceDirectory.value.toPath.relativize(f.toPath).toString
    logger.info(s"Retrieving $path...")
    val objStream = s3.getObject(s3BucketName.value, path).getObjectContent
    val fileStream = new FileInputStream((cloudFormationSourceDirectory.value / path).toPath.toFile)

    val mainSame = IOUtils.contentEquals(objStream, fileStream)
    if(!mainSame) sys.error(s"Files contents for $path did not match.")
  }
}

TaskKey[Unit]("cleanup") := {
  //Remove the test s3 bucket
  val s3 = new AmazonS3Client()
  val bucketName = s3BucketName.value
  val currentObjects = s3.listObjects(bucketName).getObjectSummaries.asScala
  currentObjects.foreach( obj => s3.deleteObject(bucketName, obj.getKey))
  s3.deleteBucket(bucketName)
}

