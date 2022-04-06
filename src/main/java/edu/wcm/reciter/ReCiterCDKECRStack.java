package edu.wcm.reciter;

import java.util.Arrays;

import software.amazon.awscdk.CfnOutput;
import software.constructs.Construct;
import software.amazon.awscdk.NestedStack;
import software.amazon.awscdk.NestedStackProps;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.RepositoryProps;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.TagMutability;
import software.amazon.awscdk.services.ecr.TagStatus;

public class ReCiterCDKECRStack extends NestedStack {

    private final Repository reciterRepo;
    private final Repository reciterPubmedRepo;
    private Repository reciterScopusRepo;
    private final Repository reciterPubManagerRepo;
    private final Repository reciterMachineLearningAnalysisRepo;
    
    public ReCiterCDKECRStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public ReCiterCDKECRStack(final Construct parent, final String id, final NestedStackProps props) {
        super(parent, id, props);


        //ReCiter ECR repo
        reciterRepo = new Repository(this, "reciterRepo", RepositoryProps.builder()
            .imageScanOnPush(true)
            .repositoryName("reciter")
            .removalPolicy(RemovalPolicy.DESTROY)
            .lifecycleRules(Arrays.asList(LifecycleRule.builder()
                .description("remove old images")
                .maxImageCount(10)
                .rulePriority(1)
                .tagStatus(TagStatus.ANY)
                .build()))
            .imageTagMutability(TagMutability.MUTABLE)
            .build());
        
        //ReCiter ECR repo
        reciterPubmedRepo = new Repository(this, "reciterPubmedRepo", RepositoryProps.builder()
            .imageScanOnPush(true)
            .repositoryName("reciter/pubmed")
            .removalPolicy(RemovalPolicy.DESTROY)
            .lifecycleRules(Arrays.asList(LifecycleRule.builder()
                .description("remove old images")
                .maxImageCount(10)
                .rulePriority(1)
                .tagStatus(TagStatus.ANY)
                .build()))
            .imageTagMutability(TagMutability.MUTABLE)
            .build());

        if(System.getenv("INCLUDE_SCOPUS") != null && System.getenv("INCLUDE_SCOPUS").equals("true")) {
            //ReCiter ECR repo
            reciterScopusRepo = new Repository(this, "reciterScopusRepo", RepositoryProps.builder()
                .imageScanOnPush(true)
                .repositoryName("reciter/scopus")
                .removalPolicy(RemovalPolicy.DESTROY)
                .lifecycleRules(Arrays.asList(LifecycleRule.builder()
                    .description("remove old images")
                    .maxImageCount(10)
                    .rulePriority(1)
                    .tagStatus(TagStatus.ANY)
                    .build()))
                .imageTagMutability(TagMutability.MUTABLE)
                .build());
        }

        //ReCiter Pub Manager ECR repo
        reciterPubManagerRepo = new Repository(this, "reciterPubManagerRepo", RepositoryProps.builder()
            .imageScanOnPush(true)
            .repositoryName("reciter/pub-manager")
            .removalPolicy(RemovalPolicy.DESTROY)
            .lifecycleRules(Arrays.asList(LifecycleRule.builder()
                .description("remove old images")
                .maxImageCount(10)
                .rulePriority(1)
                .tagStatus(TagStatus.ANY)
                .build()))
            .imageTagMutability(TagMutability.MUTABLE)
            .build());

        //ReCiter Machine Learning Analysis ECR repo
        reciterMachineLearningAnalysisRepo = new Repository(this, "reciterMachineLearningAnalysisRepo", RepositoryProps.builder()
            .imageScanOnPush(true)
            .repositoryName("reciter/machine-learning-analysis")
            .removalPolicy(RemovalPolicy.DESTROY)
            .lifecycleRules(Arrays.asList(LifecycleRule.builder()
                .description("remove old images")
                .maxImageCount(10)
                .rulePriority(1)
                .tagStatus(TagStatus.ANY)
                .build()))
            .imageTagMutability(TagMutability.MUTABLE)
            .build());

        //Tagging for all Resources
        Tags.of(this).add("application", "ReCiter");
        Tags.of(this).add("stack-id", ReCiterCDKECRStack.of(this).getStackId());
        Tags.of(this).add("stack-name", ReCiterCDKECRStack.of(this).getStackName());
        Tags.of(this).add("stack-region", ReCiterCDKECRStack.of(this).getRegion());

        CfnOutput.Builder.create(this, "ecrRepoUrlReCiter")
            .description("ECR Repo url for ReCiter")
            .exportName("ecrRepoUrlReCiter")
            .value(reciterRepo.getRepositoryUri())
            .build();
        
        CfnOutput.Builder.create(this, "ecrRepoUrlReCiterPubmed")
            .description("ECR Repo url for ReCiter-Pubmed")
            .exportName("ecrRepoUrlReCiterPubmed")
            .value(reciterPubmedRepo.getRepositoryUri())
            .build();
        
        if(System.getenv("INCLUDE_SCOPUS") != null && System.getenv("INCLUDE_SCOPUS").equals("true")) {
            CfnOutput.Builder.create(this, "ecrRepoUrlReCiterScopus")
                .description("ECR Repo url for ReCiter-Scopus")
                .exportName("ecrRepoUrlReCiterScopus")
                .value(reciterScopusRepo.getRepositoryUri())
                .build();
        }
        CfnOutput.Builder.create(this, "ecrRepoUrlReCiterPubManager")
            .description("ECR Repo url for ReCiter-Pub-Manager")
            .exportName("ecrRepoUrlReCiterPubManager")
            .value(reciterPubManagerRepo.getRepositoryUri())
            .build();
        
        CfnOutput.Builder.create(this, "ecrRepoUrlReCiterMachineLearningAnalysis")
            .description("ECR Repo url for ReCiter-Machine-Learning-Analysis")
            .exportName("ecrRepoUrlReCiterMachineLearningAnalysis")
            .value(reciterMachineLearningAnalysisRepo.getRepositoryUri())
            .build();
    }

    public Repository getReCiterEcrRepo() {
        return this.reciterRepo;
    }
    public Repository getReCiterPubmedEcrRepo() {
        return this.reciterPubmedRepo;
    }
    public Repository getReCiterScopusEcrRepo() {
        return this.reciterScopusRepo;
    }
    public Repository getReCiterPubManagerEcrRepo() {
        return this.reciterPubManagerRepo;
    }
    public Repository getReCiterMachineLearningAnalysisEcrRepo() {
        return this.reciterMachineLearningAnalysisRepo;
    }
}
