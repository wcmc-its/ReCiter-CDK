package edu.wcm.reciter;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.NestedStack;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class ReCiterCdkStack extends Stack {
    public ReCiterCdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ReCiterCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        ReCiterCDKVPCStack reCiterCDKVPCStack = new ReCiterCDKVPCStack(this, "reCiterCDKVPCStack");

        ReCiterCDKECRStack reCiterCDKECRStack = new ReCiterCDKECRStack(this, "reCiterCDKECRStack");
        NestedStack.isNestedStack(reCiterCDKECRStack);
        reCiterCDKECRStack.addDependency(reCiterCDKVPCStack, "This is dependent on VPC Resources being created");
    }
}
