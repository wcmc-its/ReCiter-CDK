package edu.wcm.reciter;

import org.json.JSONObject;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.NestedStack;
import software.amazon.awscdk.core.NestedStackProps;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretProps;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;

public class ReCiterCdkSecretsManagerStack extends NestedStack {
    
    private final ISecret reciterSecret;
    private final ISecret reciterPubmedSecret;
    private final ISecret reciterScopusSecret;

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
                    .put("AMAZON_AWS_ACCESS_KEY", "test")
                    .put("AMAZON_AWS_SECRET_KEY", "test")
                    .put("ADMIN_API_KEY", "abc")
                    .put("CONSUMER_API_KEY", "def")
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
                    .put("PUBMED_API_KEY", "pubmed-api-key")                    
                .toString())
                .generateStringKey("password")
                .build())
            .secretName("cdk-reciter-pubmed-secrets")
            .build());

        reciterScopusSecret = new Secret(this, "reciterScopusSecret", SecretProps.builder()
            .description("This contains all secrets for ReCiter-Scopus application")
            .removalPolicy(RemovalPolicy.DESTROY)
            .generateSecretString(SecretStringGenerator.builder()
                .secretStringTemplate(new JSONObject()
                    .put("SCOPUS_API_KEY", "scopus-api-key") 
                    .put("SCOPUS_INST_TOKEN", "scopus-inst-token")                    
                .toString())
                .generateStringKey("password")
                .build())
            .secretName("cdk-reciter-scopus-secrets")
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
}
