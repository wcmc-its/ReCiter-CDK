package edu.wcm.reciter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.api.Test;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class ReCiterCdkTest {
    private final static ObjectMapper JSON =
        new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    @Test
    public void testStack() throws IOException {
        App app = new App();
        ReCiterCdkStack stack = new ReCiterCdkStack(app, "masterStack", 
        StackProps.builder()
        //.env(ReCiterCdkApp.makeEnv(System.getenv("CDK_DEFAULT_ACCOUNT"), System.getenv("CDK_DEFAULT_REGION")))
        .build());
        // synthesize the stack to a CloudFormation     template
        JsonNode actual = JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());

        // Update once resources have been added to the stack
        assertThat(actual.toString().contains("AWS::CloudFormation::Stack"));
        assertThat(actual.toString().contains("AWS::Logs::LogGroup"));
    }

    @Test
    public void testEnvVariables() throws IOException {
        String adminApikey = System.getenv("ADMIN_API_KEY");
        assertNotNull(adminApikey, "ADMIN_API_KEY environment variable is required for stack to run");

        String consumerApiKey = System.getenv("CONSUMER_API_KEY");
        assertNotNull(consumerApiKey, "CONSUMER_API_KEY environment variable is required for stack to run");

        String pubmedApiKey = System.getenv("PUBMED_API_KEY");
        assertNotNull(pubmedApiKey, "PUBMED_API_KEY environment variable is required for stack to run");

        String includeScopus = System.getenv("INCLUDE_SCOPUS");
        assertNotNull(includeScopus, "INCLUDE_SCOPUS environment variable is required for stack to run");

        String alarmEmail = System.getenv("ALARM_EMAIL");
        assertNotNull(alarmEmail, "ALARM_EMAIL environment variable is required for stack to run");

        String githubUser = System.getenv("GITHUB_USER");
        assertNotNull(githubUser, "GITHUB_USER environment variable is required for stack to run");

        String githubPersonalAccessToken = System.getenv("GITHUB_PERSONAL_ACCESS_TOKEN");
        assertNotNull(githubPersonalAccessToken, "GITHUB_PERSONAL_ACCESS_TOKEN environment variable is required for stack to run");

        String scopusApiKey = System.getenv("SCOPUS_API_KEY");
        assertNotNull(scopusApiKey, "SCOPUS_API_KEY environment variable is required for stack to run");

        String scopusInstToken = System.getenv("SCOPUS_INST_TOKEN");
        assertNotNull(scopusInstToken, "SCOPUS_INST_TOKEN environment variable is required for stack to run");
    }
}
