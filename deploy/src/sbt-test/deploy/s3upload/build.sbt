import java.io.FileInputStream

import com.amazonaws.services.s3.AmazonS3Client
import org.apache.commons.io.IOUtils
import scala.collection.JavaConverters._

enablePlugins(LambdakkaDeployPlugin)

name := "deploy-test-s3"

TaskKey[Unit]("check") := {
  //Check that the file is there
  val s3 = new AmazonS3Client()

  val objStream = s3.getObject(s3BucketName.value, "main.yml").getObjectContent
  val fileStream = new FileInputStream((cloudFormationSourceDirectory.value / "main.yml").toPath.toFile)

  val same = IOUtils.contentEquals(objStream, fileStream)
  if(!same) sys.error("Files contents did not match.")

}

TaskKey[Unit]("cleanup") := {
  //Remove the test s3 bucket
  val s3 = new AmazonS3Client()
  val bucketName = s3BucketName.value
  val currentObjects = s3.listObjects(bucketName).getObjectSummaries.asScala
  currentObjects.foreach( obj => s3.deleteObject(bucketName, obj.getKey))
  s3.deleteBucket(bucketName)
}

