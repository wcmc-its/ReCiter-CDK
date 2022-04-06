package edu.wcm.reciter;

import java.util.HashMap;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

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
        Environment envReCiter = makeEnv(null, null);
        if((System.getenv("ADMIN_API_KEY") == null || System.getenv("ADMIN_API_KEY").isEmpty())
        ||
        (System.getenv("CONSUMER_API_KEY") == null || System.getenv("CONSUMER_API_KEY").isEmpty())
        ||
        (System.getenv("PUBMED_API_KEY") == null || System.getenv("PUBMED_API_KEY").isEmpty())
        ||
        (System.getenv("ALARM_EMAIL") == null || System.getenv("ALARM_EMAIL").isEmpty())
        ||
        (System.getenv("GITHUB_USER") == null || System.getenv("GITHUB_USER").isEmpty())
        ||
        (System.getenv("GITHUB_PERSONAL_ACCESS_TOKEN") == null || System.getenv("GITHUB_PERSONAL_ACCESS_TOKEN").isEmpty())
        ||
        (System.getenv("INCLUDE_SCOPUS") == null || System.getenv("INCLUDE_SCOPUS").isEmpty())
        ||
        (System.getenv("INCLUDE_SCOPUS").equals("true") && (System.getenv("SCOPUS_API_KEY") == null || System.getenv("SCOPUS_INST_TOKEN") == null))
        ) {
                System.out.println("Please set all the environment variables to run the stack. Make sure the following variables are set: \n" + 
                "ADMIN_API_KEY: The admin api key used for ReCiter\n" +
                "CONSUMER_API_KEY: The consumer api key used for retrieving publications from ReCiter\n" +
                "PUBMED_API_KEY: The pubmed api key to make sure pubmed does not throttle\n" +
                "ALARM_EMAIL: The email address where alerts for ReCiter and its components will be sent\n" +
                "GITHUB_USER: The github username where the reciter and its components are forked\n" +
                "GITHUB_PERSONAL_ACCESS_TOKEN: The personal access token to fetch the repository and create webhooks\n" +
                "INCLUDE_SCOPUS: If you have scopus subscription then do true otherwise false\n" +
                "SCOPUS_API_KEY: If INLCUDE_SCOPUS variable is true then this needs to be set\n" +
                "SCOPUS_INST_TOKEN: If INLCUDE_SCOPUS variable is true then this needs to be set\n");
                System.exit(1);
        }

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
