package edu.wcm.reciter;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.NestedStack;
import software.amazon.awscdk.core.NestedStackProps;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class ReCiterCdkStack extends Stack {
    public ReCiterCdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ReCiterCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        ReCiterCDKVPCStack reCiterCDKVPCStack = new ReCiterCDKVPCStack(this, "reCiterCDKVPCStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build());

        ReCiterCdkRDSStack reCiterCdkRDSStack = new ReCiterCdkRDSStack(this, "reCiterCdkRDSStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build(), 
            reCiterCDKVPCStack.getVpc(), reCiterCDKVPCStack.getPrivateSubnetGroup(), reCiterCDKVPCStack.getPublicSubnetGroup());
        NestedStack.isNestedStack(reCiterCdkRDSStack);    
        reCiterCdkRDSStack.addDependency(reCiterCDKVPCStack, "RDS is dependent on VPC Stack");

        ReCiterCdkSecretsManagerStack reCiterCdkSecretsManagerStack = new ReCiterCdkSecretsManagerStack(this, "reCiterCdkSecretsManagerStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build());
        NestedStack.isNestedStack(reCiterCdkSecretsManagerStack);

        ReCiterCDKECRStack reCiterCDKECRStack = new ReCiterCDKECRStack(this, "reCiterCDKECRStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build());
        NestedStack.isNestedStack(reCiterCDKECRStack);
        reCiterCDKECRStack.addDependency(reCiterCDKVPCStack, "This is dependent on VPC Resources being created");

        ReCiterCDKECSStack reCiterCDKECSStack = new ReCiterCDKECSStack(this, "reCiterCDKECSStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build(),
            reCiterCDKVPCStack.getVpc(), reCiterCdkSecretsManagerStack.getReCiterSecret(), reCiterCdkSecretsManagerStack.getReCiterPubmedSecret(), reCiterCdkSecretsManagerStack.getReCiterScopusSecret()
            , reCiterCDKECRStack.getReCiterEcrRepo(), reCiterCDKECRStack.getReCiterPubmedEcrRepo(), reCiterCDKECRStack.getReCiterScopusEcrRepo(), reCiterCDKECRStack.getReCiterPubManagerEcrRepo(), reCiterCDKECRStack.getReCiterMachineLearningAnalysisEcrRepo(), reCiterCdkRDSStack.getReciterDb());
        NestedStack.isNestedStack(reCiterCDKECSStack);
        reCiterCDKECSStack.addDependency(reCiterCdkRDSStack, "RDS is needed for Scheduled task");
        reCiterCDKECSStack.addDependency(reCiterCdkSecretsManagerStack, "ECS Stack is dependent on SecretsManager Stack");
        reCiterCDKECSStack.addDependency(reCiterCDKVPCStack, "ECS Stack is dependent on VPC");
        reCiterCDKECSStack.addDependency(reCiterCDKECRStack, "ECR repo is needed for ECS Tasks");
        

        ReCiterCdkPipelineStack reCiterCdkPipelineStack = new ReCiterCdkPipelineStack(this, "reCiterCdkCodeBuildStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build(), 
            reCiterCDKECSStack.getCluster(), reCiterCDKECRStack.getReCiterEcrRepo(), reCiterCDKECRStack.getReCiterPubmedEcrRepo(), reCiterCDKECRStack.getReCiterScopusEcrRepo(), reCiterCDKECRStack.getReCiterPubManagerEcrRepo(), reCiterCDKECRStack.getReCiterMachineLearningAnalysisEcrRepo(),
            reCiterCDKECSStack.getReCiterTopic(), 
            reCiterCDKECSStack.getReCiterPubmedService(), reCiterCDKECSStack.getReCiterScopusService(), reCiterCDKECSStack.getReCiterService(), reCiterCDKECSStack.getReCiterPubManagerService(),
            reCiterCdkSecretsManagerStack.getReCiterSecret(), reCiterCDKECSStack.getAlb(), reCiterCDKVPCStack.getVpc());
        NestedStack.isNestedStack(reCiterCdkPipelineStack);
        reCiterCdkPipelineStack.addDependency(reCiterCDKECSStack);

        ReCiterCdkWAFStack reCiterCdkWAFStack = new ReCiterCdkWAFStack(this, "reCiterCdkWAFStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build(), 
            reCiterCDKECSStack.getAlb());
        NestedStack.isNestedStack(reCiterCdkWAFStack);
        reCiterCdkWAFStack.addDependency(reCiterCDKECSStack, "WAF dependent on ALB");
        
    }
}
