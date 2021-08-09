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

        ReCiterCdkSecretsManagerStack reCiterCdkSecretsManagerStack = new ReCiterCdkSecretsManagerStack(this, "reCiterCdkSecretsManagerStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build());
        NestedStack.isNestedStack(reCiterCdkSecretsManagerStack);

        ReCiterCdkRDSStack reCiterCdkRDSStack = new ReCiterCdkRDSStack(this, "reCiterCdkRDSStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build(), 
            reCiterCDKVPCStack.getVpc(), reCiterCDKVPCStack.getPrivateSubnetGroup());
        reCiterCdkRDSStack.addDependency(reCiterCDKVPCStack, "RDS is dependent on VPC Stack");
        

        ReCiterCDKECRStack reCiterCDKECRStack = new ReCiterCDKECRStack(this, "reCiterCDKECRStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build());
        NestedStack.isNestedStack(reCiterCDKECRStack);
        reCiterCDKECRStack.addDependency(reCiterCDKVPCStack, "This is dependent on VPC Resources being created");

        ReCiterCDKECSStack reCiterCDKECSStack = new ReCiterCDKECSStack(this, "reCiterCDKECSStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build(),
            reCiterCDKVPCStack.getVpc(), reCiterCdkSecretsManagerStack.getReCiterSecret(), reCiterCdkSecretsManagerStack.getReCiterPubmedSecret(), reCiterCdkSecretsManagerStack.getReCiterScopusSecret()
            , reCiterCDKECRStack.getReCiterEcrRepo(), reCiterCDKECRStack.getReCiterPubmedEcrRepo(), reCiterCDKECRStack.getReCiterScopusEcrRepo(), reCiterCDKECRStack.getReCiterPubManagerEcrRepo());
        NestedStack.isNestedStack(reCiterCDKECSStack);
        reCiterCDKECSStack.addDependency(reCiterCdkSecretsManagerStack, "ECS Stack is dependent on SecretsManager Stack");
        reCiterCDKECSStack.addDependency(reCiterCDKVPCStack, "ECS Stack is dependent on VPC");
        reCiterCDKECSStack.addDependency(reCiterCDKECRStack, "ECR repo is needed for ECS Tasks");
        

        ReCiterCdkWAFStack reCiterCdkWAFStack = new ReCiterCdkWAFStack(this, "reCiterCdkWAFStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build(), 
            reCiterCDKECSStack.getAlb());
        NestedStack.isNestedStack(reCiterCdkWAFStack);
        reCiterCdkWAFStack.addDependency(reCiterCDKECSStack, "WAF dependent on ALB");
        
    }
}
