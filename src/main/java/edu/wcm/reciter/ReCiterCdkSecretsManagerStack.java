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
    private ISecret reciterScopusSecret;

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
