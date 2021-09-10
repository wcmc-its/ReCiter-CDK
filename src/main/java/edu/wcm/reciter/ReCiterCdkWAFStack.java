package edu.wcm.reciter;

import java.util.Arrays;
import java.util.HashMap;

import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.NestedStack;
import software.amazon.awscdk.core.NestedStackProps;
import software.amazon.awscdk.core.Tags;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.wafv2.CfnWebACL;
import software.amazon.awscdk.services.wafv2.CfnWebACL.AllowActionProperty;
import software.amazon.awscdk.services.wafv2.CfnWebACL.DefaultActionProperty;
import software.amazon.awscdk.services.wafv2.CfnWebACL.ExcludedRuleProperty;
import software.amazon.awscdk.services.wafv2.CfnWebACL.ManagedRuleGroupStatementProperty;
import software.amazon.awscdk.services.wafv2.CfnWebACL.OverrideActionProperty;
import software.amazon.awscdk.services.wafv2.CfnWebACL.RuleProperty;
import software.amazon.awscdk.services.wafv2.CfnWebACL.StatementProperty;
import software.amazon.awscdk.services.wafv2.CfnWebACL.VisibilityConfigProperty;
import software.amazon.awscdk.services.wafv2.CfnWebACLAssociation;
import software.amazon.awscdk.services.wafv2.CfnWebACLAssociationProps;
import software.amazon.awscdk.services.wafv2.CfnWebACLProps;

public class ReCiterCdkWAFStack extends NestedStack {
    
    public ReCiterCdkWAFStack(final Construct parent, final String id) {
        this(parent, id, null, null);
    }

    public ReCiterCdkWAFStack(final Construct parent, final String id, final NestedStackProps props, ApplicationLoadBalancer alb) {
        super(parent, id, props);

        //WAF for ReCiter ALB
        final CfnWebACL reCiterWaf = new CfnWebACL(this, "reCiterWaf", CfnWebACLProps.builder()
            .description("This is WAF for ReCiter and its services")
            .scope("REGIONAL")
            .name("cdk-reciter-waf")
            .defaultAction(DefaultActionProperty.builder()
                .allow(AllowActionProperty.builder().build())
                .build())
            .visibilityConfig(VisibilityConfigProperty.builder()
                .cloudWatchMetricsEnabled(true)
                .sampledRequestsEnabled(true)
                .metricName("cdk-reciter-metric")
                .build())
            .rules(Arrays.asList(
                RuleProperty.builder()
                    .name("AWSManagedRulesCommonRuleSet")
                    .priority(0)
                    .overrideAction(OverrideActionProperty.builder().none(new HashMap<String, Object>()).build())
                    .visibilityConfig(VisibilityConfigProperty.builder()
                        .cloudWatchMetricsEnabled(true)
                        .sampledRequestsEnabled(true)
                        .metricName("cdk-reciter-AWSManagedRulesCommonRuleSetMetric")
                        .build())
                    .statement(StatementProperty.builder()
                        .managedRuleGroupStatement(ManagedRuleGroupStatementProperty.builder()
                            .vendorName("AWS")
                            .name("AWSManagedRulesCommonRuleSet")
                            .excludedRules(Arrays.asList(ExcludedRuleProperty.builder().name("GenericRFI_QUERYARGUMENTS").build(), ExcludedRuleProperty.builder().name("SizeRestrictions_BODY").build()))
                            .build())
                        .build())
                    .build(),
                RuleProperty.builder()
                    .name("AWSManagedRulesLinuxRuleSet")
                    .priority(1)
                    .overrideAction(OverrideActionProperty.builder().none(new HashMap<String, Object>()).build())
                    .visibilityConfig(VisibilityConfigProperty.builder()
                        .cloudWatchMetricsEnabled(true)
                        .sampledRequestsEnabled(true)
                        .metricName("cdk-reciter-AWSManagedRulesLinuxRuleSetMetric")
                        .build())
                    .statement(StatementProperty.builder()
                        .managedRuleGroupStatement(ManagedRuleGroupStatementProperty.builder()
                            .vendorName("AWS")
                            .name("AWSManagedRulesLinuxRuleSet")
                            .excludedRules(Arrays.asList())
                            .build())
                        .build())
                    .build(),
                RuleProperty.builder()
                    .name("AWSManagedRulesSQLiRuleSet")
                    .priority(2)
                    .overrideAction(OverrideActionProperty.builder().none(new HashMap<String, Object>()).build())
                    .visibilityConfig(VisibilityConfigProperty.builder()
                        .cloudWatchMetricsEnabled(true)
                        .sampledRequestsEnabled(true)
                        .metricName("cdk-reciter-AWSManagedRulesSQLiRuleSetMetric")
                        .build())
                    .statement(StatementProperty.builder()
                        .managedRuleGroupStatement(ManagedRuleGroupStatementProperty.builder()
                            .vendorName("AWS")
                            .name("AWSManagedRulesSQLiRuleSet")
                            .excludedRules(Arrays.asList(ExcludedRuleProperty.builder().name("SQLi_COOKIE").build()))
                            .build())
                        .build())
                    .build(),
                    RuleProperty.builder()
                        .name("AWSManagedRulesKnownBadInputsRuleSet")
                        .priority(3)
                        .overrideAction(OverrideActionProperty.builder().none(new HashMap<String, Object>()).build())
                        .visibilityConfig(VisibilityConfigProperty.builder()
                            .cloudWatchMetricsEnabled(true)
                            .sampledRequestsEnabled(true)
                            .metricName("cdk-reciter-AWSManagedRulesKnownBadInputsRuleSetMetric")
                            .build())
                        .statement(StatementProperty.builder()
                            .managedRuleGroupStatement(ManagedRuleGroupStatementProperty.builder()
                                .vendorName("AWS")
                                .name("AWSManagedRulesKnownBadInputsRuleSet")
                                .excludedRules(Arrays.asList())
                                .build())
                            .build())
                        .build()
                )
            )
            .build());
        
        //Associate WAF with ALB

        final CfnWebACLAssociation reCiterCfnWebACLAssociation = new CfnWebACLAssociation(this, "reCiterCfnWebACLAssociation", CfnWebACLAssociationProps.builder()
            .webAclArn(reCiterWaf.getAttrArn())
            .resourceArn(alb.getLoadBalancerArn())
            .build());

        //Tagging for all Resources
        Tags.of(this).add("application", "ReCiter");
        Tags.of(this).add("stack-name", ReCiterCdkWAFStack.of(this).getStackName());
        Tags.of(this).add("stack-region", ReCiterCdkWAFStack.of(this).getRegion());

        CfnOutput.Builder.create(this, "reCiterWafArn")
        .description("WAF Arn for ReCiter")
        .exportName("reCiterWafArn")
        .value(reCiterWaf.getAttrArn())
        .build();

        CfnOutput.Builder.create(this, "reCiterAlbArn")
        .description("ARN For ALB attached to WAF")
        .exportName("reCiterAlbArn")
        .value(reCiterCfnWebACLAssociation.getResourceArn())
        .build();
    }
}
