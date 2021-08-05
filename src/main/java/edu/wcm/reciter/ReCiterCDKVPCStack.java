package edu.wcm.reciter;

import java.util.Arrays;
import java.util.HashMap;

import software.amazon.awscdk.core.Aspects;
import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.NestedStack;
import software.amazon.awscdk.core.NestedStackProps;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Tag;
import software.amazon.awscdk.core.Tags;
import software.amazon.awscdk.services.ec2.FlowLogDestination;
import software.amazon.awscdk.services.ec2.FlowLogOptions;
import software.amazon.awscdk.services.ec2.FlowLogTrafficType;
import software.amazon.awscdk.services.ec2.NatProvider;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcProps;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.rds.SubnetGroup;
import software.amazon.awscdk.services.rds.SubnetGroupProps;

public class ReCiterCDKVPCStack extends NestedStack {
    
    public ReCiterCDKVPCStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public ReCiterCDKVPCStack(final Construct parent, final String id, final NestedStackProps props) {
        super(parent, id, props);

        //ReCiter VPC
        final Vpc reciterVpc = new Vpc(this, "reciterVpc", VpcProps.builder()
            .cidr("10.0.0.0/16")
            .natGateways(2)
            .maxAzs(2)
            .subnetConfiguration(Arrays.asList(SubnetConfiguration.builder().cidrMask(24).name("reciter-public-subnet").subnetType(SubnetType.PUBLIC).build(),
                SubnetConfiguration.builder().cidrMask(24).name("reciter-private-app-subnet").subnetType(SubnetType.PRIVATE).build(),
                SubnetConfiguration.builder().cidrMask(28).name("reciter-private-db-subnet").subnetType(SubnetType.PRIVATE).build()))
            .enableDnsHostnames(true)
            .flowLogs(new HashMap(){{
                put("reciter-flow-logs", FlowLogOptions.builder()
                    .trafficType(FlowLogTrafficType.ALL)
                    .destination(FlowLogDestination.toCloudWatchLogs(new LogGroup(parent, "reciterVpcFlowLogs", LogGroupProps.builder()
                        .logGroupName("reciter-vpc-flowlogs")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .retention(RetentionDays.ONE_MONTH)
                        .build())))
                    .build());
            }})
            .natGatewayProvider(NatProvider.gateway())
            .enableDnsSupport(true)
            .natGatewaySubnets(SubnetSelection.builder().onePerAz(true).subnetType(SubnetType.PUBLIC).build())
            .build());

        final SubnetGroup privateSubnetGroup = new SubnetGroup(this, "privateSubnetGroup", SubnetGroupProps.builder()
            .description("This subnet group is for private subnets in ReCiter VPC")
            .removalPolicy(RemovalPolicy.DESTROY)
            .subnetGroupName("reciter-private-subnet-group")
            .vpc(reciterVpc)
            .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE).build())
            .build());
        
        
        final SubnetGroup publicSubnetGroup = new SubnetGroup(this, "publicSubnetGroup", SubnetGroupProps.builder()
            .description("This subnet group is for public subnets in ReCiter VPC")
            .removalPolicy(RemovalPolicy.DESTROY)
            .subnetGroupName("reciter-public-subnet-group")
            .vpc(reciterVpc)
            .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
            .build());

        Aspects.of(reciterVpc).add(new Tag("Name", "reciter-vpc"));

        //Tagging for all Resources
        Tags.of(this).add("application", "ReCiter");
        Tags.of(this).add("stack-id", ReCiterCDKECRStack.of(this).getStackId());
        Tags.of(this).add("stack-name", ReCiterCDKECRStack.of(this).getStackName());
        Tags.of(this).add("stack-region", ReCiterCDKECRStack.of(this).getRegion());

        CfnOutput.Builder.create(this, "vpcName")
            .description("VPC Name for ReCiter")
            .exportName("vpcName")
            .value(reciterVpc.getVpcId())
            .build();
        
        CfnOutput.Builder.create(this, "vpcPrivateSubnets")
            .description("VPC Private Subnets for ReCiter")
            .exportName("vpcPrivateSubnets")
            .value(reciterVpc.getPrivateSubnets().toString())
            .build();

        CfnOutput.Builder.create(this, "vpcPublicSubnets")
            .description("VPC Public Subnets for ReCiter")
            .exportName("vpcPublicSubnets")
            .value(reciterVpc.getPublicSubnets().toString())
            .build();

        CfnOutput.Builder.create(this, "vpcPublicSubnetGroup")
            .description("VPC Public Subnets group for ReCiter")
            .exportName("vpcPublicSubnetGroup")
            .value(publicSubnetGroup.getSubnetGroupName())
            .build();

        CfnOutput.Builder.create(this, "vpcPrivateSubnetGroup")
            .description("VPC Private Subnets group for ReCiter")
            .exportName("vpcPrivateSubnetGroup")
            .value(privateSubnetGroup.getSubnetGroupName())
            .build();
    }
}
