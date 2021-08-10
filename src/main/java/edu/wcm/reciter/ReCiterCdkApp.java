package edu.wcm.reciter;

import java.util.HashMap;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class ReCiterCdkApp {

    // Helper method to build an environment
    static Environment makeEnv(String account, String region) {
        account = (account == null) ? System.getenv("CDK_DEFAULT_ACCOUNT") : account;
        region = (region == null) ? System.getenv("CDK_DEFAULT_REGION") : region;

        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }
    public static void main(final String[] args) {
        App app = new App();
        Environment envReCiter = makeEnv(null, "us-east-2");

        new ReCiterCdkStack(app, "ReCiterCdkStack", StackProps.builder()
                // If you don't specify 'env', this stack will be environment-agnostic.
                // Account/Region-dependent features and context lookups will not work,
                // but a single synthesized template can be deployed anywhere.

                // Uncomment the next block to specialize this stack for the AWS Account
                // and Region that are implied by the current CLI configuration.
               /*
                .env(Environment.builder()
                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                        .region(System.getenv("CDK_DEFAULT_REGION"))
                        .build())
                */

                // Uncomment the next block if you know exactly what Account and Region you
                // want to deploy the stack to.
                /*
                .env(Environment.builder()
                        .account("123456789012")
                        .region("us-east-1")
                        .build())
                */

                // For more information, see https://docs.aws.amazon.com/cdk/latest/guide/environments.html
                .analyticsReporting(true)
                .description("This stack is the master stack that will build VPC, ECR, ECS, WAF, SecretsManager, CodePipeline, Codebuild for ReCiter and its components.")
                .terminationProtection(false)
                .stackName("ReCiterCdkMasterStack")
                .tags(new HashMap<String, String>(){{
                        put("application", "ReCiter");
                        put("cdk-maintainer", "Sarbajit Dutta - szd2013@med.cornell.edu");
                }})
                .env(envReCiter)
                .build());

        app.synth();
    }
}
