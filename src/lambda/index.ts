import { sendPubEmailNotifications } from "./notifications/notifications.sendEmail.controller";

const assert = require('assert');
const http = require('http');
const AWS = require('aws-sdk');

export async function handler(event: any, context: any): Promise<any> {
  const codepipeline = new AWS.CodePipeline();	
  //const data = await sendPubEmailNotifications();
    
    // Retrieve the Job ID from the Lambda action
   /* var jobId = event["CodePipeline.job"].id;
    console.log('event************', event);
    // Retrieve the value of UserParameters from the Lambda action configuration in CodePipeline, in this case a URL which will be
    // health checked by this function.
	 // Notify CodePipeline of a successful job
    /*var putJobSuccess = function(message:any) {
        var params = {
            jobId: jobId
        };
		console.log('params',params)
		try
		{
			codepipeline.putJobSuccessResult(params, function(err:any, data:any) {
				console.log('err and data',err,data);
				if(err) {
					//context.fail(err);
					console.log(err);		
				} else {
					//context.succeed(message);  
					console.log(data)		
				}
			});
		}catch(ex)
		{
			console.log("Exception*************",ex);
		}
		
    };
    
    // Notify CodePipeline of a failed job
    var putJobFailure = function(message:any) {
	    var params = {
            jobId: jobId,
            failureDetails: {
                message: JSON.stringify(message),
                type: 'JobFailed',
                externalExecutionId: context.awsRequestId
            }
        };
		console.log('params from fail',params)
        codepipeline.putJobFailureResult(params, function(err:any, data:any) {
			console.log('err and data from fail',err,data);
            context.fail(message);      
        });
    };*/

    try { 
        const data = await sendPubEmailNotifications();
		console.log("Completed Job execution and moving to Job Success",data);
        // Succeed the job
       // putJobSuccess("Tests passed.");
    } catch (ex) {
        // If any of the assertions failed then fail the job
		console.log("Completed Job execution and moving to Job failed",ex);
       // putJobFailure(ex);    
    }   
}



