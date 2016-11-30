# Lambdakka core

This is the runtime module used for running Akka HTTP routes

You can mix in the `HttpLambdaExecutorTrait` and join the BidiFlow provided by the `lambdaLayer` method
with an akka HTTP route. You can then use the `runFlow` method to run your service with input and output streams from
the aws lambda function.