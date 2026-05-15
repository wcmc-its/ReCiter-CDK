package edu.wcm.reciter;

import org.json.JSONObject;

import software.constructs.Construct;
import software.amazon.awscdk.NestedStack;
import software.amazon.awscdk.NestedStackProps;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretProps;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;

public class ReCiterCdkSecretsManagerStack extends NestedStack {

    private final ISecret reciterSecret;
    private final ISecret reciterPubmedSecret;
    private ISecret reciterScopusSecret;
    private final ISecret reciterAIOpenAISecret;
    private final ISecret reciterAITeamsWebhookSecret;

    public ReCiterCdkSecretsManagerStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public ReCiterCdkSecretsManagerStack(final Construct parent, final String id, final NestedStackProps props) {
        super(parent, id, props);

        reciterSecret = new Secret(this, "reciterSecret", SecretProps.builder()
            .description("This contains all secrets for ReCiter application")
            .removalPolicy(RemovalPolicy.DESTROY)
            .generateSecretString(SecretStringGenerator.builder()
                .secretStringTemplate(new JSONObject()
                    .put("ADMIN_API_KEY", System.getenv("ADMIN_API_KEY"))
                    .put("CONSUMER_API_KEY", System.getenv("CONSUMER_API_KEY"))
                    .put("AWS_REGION", ReCiterCdkSecretsManagerStack.of(this).getRegion())
                    .put("SERVER_PORT", 5000)
                .toString())
                .generateStringKey("password")
                .build())
            .secretName("cdk-reciter-secrets")
            .build());

        reciterPubmedSecret = new Secret(this, "reciterPubmedSecret", SecretProps.builder()
            .description("This contains all secrets for ReCiter-Pubmed application")
            .removalPolicy(RemovalPolicy.DESTROY)
            .generateSecretString(SecretStringGenerator.builder()
                .secretStringTemplate(new JSONObject()
                    .put("PUBMED_API_KEY", System.getenv("PUBMED_API_KEY"))                    
                .toString())
                .generateStringKey("password")
                .build())
            .secretName("cdk-reciter-pubmed-secrets")
            .build());
        
        if(System.getenv("INCLUDE_SCOPUS") != null && System.getenv("INCLUDE_SCOPUS").equals("true")) {
            reciterScopusSecret = new Secret(this, "reciterScopusSecret", SecretProps.builder()
                .description("This contains all secrets for ReCiter-Scopus application")
                .removalPolicy(RemovalPolicy.DESTROY)
                .generateSecretString(SecretStringGenerator.builder()
                    .secretStringTemplate(new JSONObject()
                        .put("SCOPUS_API_KEY", System.getenv("SCOPUS_API_KEY"))
                        .put("SCOPUS_INST_TOKEN", System.getenv("SCOPUS_INST_TOKEN"))
                    .toString())
                    .generateStringKey("password")
                    .build())
                .secretName("cdk-reciter-scopus-secrets")
                .build());
        }

        // ReciterAI daily-enrichment secrets. These are single-value secrets
        // (raw API key, raw webhook URL) — not JSON envelopes — so the operator
        // can update with `aws secretsmanager update-secret --secret-string '<raw>'`
        // without worrying about JSON quoting. CDK auto-generates a random initial
        // value on stack create; the operator overwrites it post-deploy. The real
        // value never lives in CDK source or in CloudFormation events.
        reciterAIOpenAISecret = new Secret(this, "reciterAIOpenAISecret", SecretProps.builder()
            .description("OpenAI API key for the ReciterAI daily-enrichment job. Populate post-deploy with: aws secretsmanager update-secret --secret-id reciterai/openai-api-key --secret-string '<sk-...>'")
            .removalPolicy(RemovalPolicy.DESTROY)
            .secretName("reciterai/openai-api-key")
            .build());

        reciterAITeamsWebhookSecret = new Secret(this, "reciterAITeamsWebhookSecret", SecretProps.builder()
            .description("Microsoft Teams Incoming Webhook URL for ReciterAI alerts. Populate post-deploy with: aws secretsmanager update-secret --secret-id reciterai/teams-webhook-url --secret-string '<https://...>'")
            .removalPolicy(RemovalPolicy.DESTROY)
            .secretName("reciterai/teams-webhook-url")
            .build());
    }

    public ISecret getReCiterSecret() {
        return this.reciterSecret;
    }

    public ISecret getReCiterPubmedSecret() {
        return this.reciterPubmedSecret;
    }

    public ISecret getReCiterScopusSecret() {
        return this.reciterScopusSecret;
    }

    public ISecret getReciterAIOpenAISecret() {
        return this.reciterAIOpenAISecret;
    }

    public ISecret getReciterAITeamsWebhookSecret() {
        return this.reciterAITeamsWebhookSecret;
    }
}
