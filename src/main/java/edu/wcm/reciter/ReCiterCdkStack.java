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

        ReCiterCDKECRStack reCiterCDKECRStack = new ReCiterCDKECRStack(this, "reCiterCDKECRStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build());
        NestedStack.isNestedStack(reCiterCDKECRStack);
        reCiterCDKECRStack.addDependency(reCiterCDKVPCStack, "This is dependent on VPC Resources being created");

        ReCiterCDKECSStack reCiterCDKECSStack = new ReCiterCDKECSStack(this, "reCiterCDKECSStack", NestedStackProps.builder()
            .removalPolicy(RemovalPolicy.DESTROY)
            .build(),
            reCiterCDKVPCStack.getVpc());
        NestedStack.isNestedStack(reCiterCDKECSStack);
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
