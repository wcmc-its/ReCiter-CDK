package edu.wcm.reciter;

import java.util.Arrays;
import java.util.HashMap;

import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.NestedStack;
import software.amazon.awscdk.core.NestedStackProps;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Tags;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.cloudwatch.Alarm;
import software.amazon.awscdk.services.cloudwatch.AlarmProps;
import software.amazon.awscdk.services.cloudwatch.TreatMissingData;
import software.amazon.awscdk.services.cloudwatch.actions.SnsAction;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AwsLogDriver;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ClusterProps;
import software.amazon.awscdk.services.ecs.ContainerDefinition;
import software.amazon.awscdk.services.ecs.ContainerDefinitionProps;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.CpuUtilizationScalingProps;
import software.amazon.awscdk.services.ecs.FargatePlatformVersion;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateServiceProps;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.FargateTaskDefinitionProps;
import software.amazon.awscdk.services.ecs.HealthCheck;
import software.amazon.awscdk.services.ecs.MemoryUtilizationScalingProps;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.PropagatedTagSource;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.ScalableTaskCount;
import software.amazon.awscdk.services.ecs.Secret;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationActionProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocolVersion;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.FixedResponseOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.IpAddressType;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerAction;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerCondition;
import software.amazon.awscdk.services.elasticloadbalancingv2.RedirectOptions;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.PolicyStatementProps;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.sns.Subscription;
import software.amazon.awscdk.services.sns.SubscriptionProps;
import software.amazon.awscdk.services.sns.SubscriptionProtocol;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.TopicProps;

public class ReCiterCDKECSStack extends NestedStack {

    private final ApplicationLoadBalancer reCiterEcsALB;
    private final Cluster reCiterCluster;
    private final Topic reciterAlarmTopic;
    private FargateService reCiterPubmedService;
    private FargateService reCiterService;
    private FargateService reCiterScopusService;
    private FargateService reCiterPubManagerService;
    
    
    public ReCiterCDKECSStack(final Construct parent, final String id) {
        this(parent, id, null, null, null, null, null, null, null, null, null);
    }

    public ReCiterCDKECSStack(final Construct parent, final String id, final NestedStackProps props, IVpc vpc, ISecret reCiterSecret, ISecret reciterPubmedSecret, ISecret reciterScopusSecret, Repository reCiterEcrRepo, Repository reCiterPubmedEcrRep, Repository reCiterScopusEcrRepo, Repository reCiterPubManagerEcrRepo) {
        super(parent, id, props);

        final SecurityGroup albSg = new SecurityGroup(this, "reciter-cdk-alb-sg", SecurityGroupProps.builder().allowAllOutbound(true)
                .description("Allow all inbound connection to application from 80 and 443")
                .vpc(vpc)
                .build());

        albSg.addIngressRule(Peer.anyIpv4(), Port.tcp(80), "Allow all Ipv4 connection from Port 80");
        albSg.addIngressRule(Peer.anyIpv4(), Port.tcp(443), "Allow all Ipv4 connection from Port 443");

        //Shared public Application Load balancer
        reCiterEcsALB = new ApplicationLoadBalancer(this, "reCiterEcsALB", ApplicationLoadBalancerProps.builder()
            .vpc(vpc)
             .vpcSubnets(SubnetSelection.builder()
                .onePerAz(true)
                .subnetType(SubnetType.PUBLIC)
                .build()) 
            .deletionProtection(false)
            .idleTimeout(Duration.seconds(60))
            .http2Enabled(true)
            .internetFacing(true)
            .ipAddressType(IpAddressType.IPV4)
            .loadBalancerName("reciter-cdk-ecs-public-alb")
            .securityGroup(albSg)
            .build());

        //Http Listener
        ApplicationListener reciterListener = reCiterEcsALB.addListener("reciter-lb-listener-http", BaseApplicationListenerProps.builder()
            .port(80)
            .protocol(ApplicationProtocol.HTTP)
            .defaultAction(ListenerAction.fixedResponse(200, FixedResponseOptions.builder()
                .contentType("text/html")
                .messageBody("<b>No Routes found</b> <p>Valid routes are /reciter /pubmed /scopus /login</p>")
                .build()))
            .build());


        //ReCiter Cluster
        reCiterCluster = new Cluster(this, "reCiterCluster", ClusterProps.builder()
            .clusterName("reCiter")
            .containerInsights(true)
            .enableFargateCapacityProviders(true)
            .vpc(vpc)
            .build());

        final FargateTaskDefinition reCiterTaskDefinition = new FargateTaskDefinition(this, "reCiterFargateTaskDef", FargateTaskDefinitionProps.builder()
            .cpu(1024)
            .memoryLimitMiB(2048)
            .build());

        reCiterTaskDefinition.addToTaskRolePolicy(new PolicyStatement(PolicyStatementProps.builder()
            .actions(Arrays.asList("dynamodb:*", "s3:*"))
            .effect(Effect.ALLOW)
            .resources(Arrays.asList("*"))
            .sid("DynamoDbS3FullAccess")
            .build()));

        reCiterEcrRepo.grantPull(reCiterTaskDefinition.obtainExecutionRole());

        ContainerDefinition reCiterNginxContainer = reCiterTaskDefinition.addContainer("reCiterNginxContainer", ContainerDefinitionProps.builder()
            .image(ContainerImage.fromRegistry("wcmcits/reciter-nginx:latest"))
            .logging(new AwsLogDriver(AwsLogDriverProps.builder()
                .logGroup(new LogGroup(this, "reciterNginxLogGroup", LogGroupProps.builder()
                    .logGroupName("/ecs/reciter/nginx")
                    .removalPolicy(RemovalPolicy.DESTROY)
                    .retention(RetentionDays.ONE_MONTH)
                    .build()))
                .streamPrefix("reciter-logs")
                .build()))
            .containerName("reciter-nginx")
            .healthCheck(HealthCheck.builder()
                .command(Arrays.asList("CMD-SHELL", "curl -f http://localhost/nginx-health || exit 1"))
                .interval(Duration.minutes(5))
                .retries(2)
                .startPeriod(Duration.seconds(60))
                .timeout(Duration.seconds(30))
                .build())
            .memoryReservationMiB(400)
            .memoryLimitMiB(548)
            .taskDefinition(reCiterTaskDefinition)
            .cpu(200)
            .build());

        
        ContainerDefinition reCiterContainer = reCiterTaskDefinition.addContainer("reCiterContainer", ContainerDefinitionProps.builder()
            .image(ContainerImage.fromRegistry("wcmcits/reciter:latest"))
            .logging(new AwsLogDriver(AwsLogDriverProps.builder()
                .logGroup(new LogGroup(this, "reciterLogGroup", LogGroupProps.builder()
                    .logGroupName("/ecs/reciter")
                    .removalPolicy(RemovalPolicy.DESTROY)
                    .retention(RetentionDays.ONE_MONTH)
                    .build()))
                .streamPrefix("reciter-logs")
                .build()))
            .containerName("reciter")
            .healthCheck(HealthCheck.builder()
                .command(Arrays.asList("CMD-SHELL", "wget --spider -S http://localhost:5000/reciter/ping || exit 1"))
                .interval(Duration.minutes(5))
                .retries(2)
                .startPeriod(Duration.seconds(120))
                .timeout(Duration.seconds(30))
                .build())
            .memoryReservationMiB(1500)
            .memoryLimitMiB(1600)
            .environment(new HashMap<String, String>(){{
                put("PUBMED_SERVICE", "http://" + reCiterEcsALB.getLoadBalancerDnsName());
                put("SCOPUS_SERVICE", "http://" + reCiterEcsALB.getLoadBalancerDnsName());
            }})
            .secrets(new HashMap<String, Secret>(){{
                //put("AMAZON_AWS_ACCESS_KEY", Secret.fromSecretsManager(reCiterSecret, "AMAZON_AWS_ACCESS_KEY"));
                //put("AMAZON_AWS_SECRET_KEY", Secret.fromSecretsManager(reCiterSecret, "AMAZON_AWS_SECRET_KEY"));
                put("ADMIN_API_KEY", Secret.fromSecretsManager(reCiterSecret, "ADMIN_API_KEY"));
                put("CONSUMER_API_KEY", Secret.fromSecretsManager(reCiterSecret, "CONSUMER_API_KEY"));
                put("AWS_REGION", Secret.fromSecretsManager(reCiterSecret, "AWS_REGION"));
                put("SERVER_PORT", Secret.fromSecretsManager(reCiterSecret, "SERVER_PORT"));
            }})
            .taskDefinition(reCiterTaskDefinition)
            .cpu(824)
            .build());

            
        
        reCiterNginxContainer.addPortMappings(PortMapping.builder()
            .containerPort(80)
            .protocol(Protocol.TCP)
            .build());
        reCiterContainer.addPortMappings(PortMapping.builder()
            .containerPort(5000)
            .protocol(Protocol.TCP)
            .build());

        

        final SecurityGroup reciterClusterSg = new SecurityGroup(this, "reciter-cdk-cluster-sg", SecurityGroupProps.builder().allowAllOutbound(true)
            .description("Allow inbound connection to application from alb")
            .allowAllOutbound(true)
            .securityGroupName("reciter-cdk-cluster-sg")
            .vpc(vpc)
            .build());

        reciterClusterSg.getConnections().allowFrom(reCiterEcsALB, Port.tcp(80), "Allow reciter ALB TCP connection from Port 5000");
        
        reCiterService = new FargateService(this, "reCiterFargateService", FargateServiceProps.builder()
            .cluster(reCiterCluster)
            .taskDefinition(reCiterTaskDefinition)
            .desiredCount(1)
            .serviceName("reciter")
            .assignPublicIp(false)
            .enableEcsManagedTags(true)
            .healthCheckGracePeriod(Duration.seconds(60))
            .platformVersion(FargatePlatformVersion.LATEST)
            .propagateTags(PropagatedTagSource.SERVICE)
            .vpcSubnets(SubnetSelection.builder()
                .onePerAz(true)
                .subnetType(SubnetType.PRIVATE)
                .build())
            .securityGroups(Arrays.asList(reciterClusterSg))
            .build());

        reCiterService.getConnections().allowFrom(reCiterEcsALB, Port.tcp(80), "Connection from ALB over port 80");

        final FargateTaskDefinition reCiterPubmedTaskDefinition = new FargateTaskDefinition(this, "reCiterPubmedTaskDefinition", FargateTaskDefinitionProps.builder()
            .cpu(1024)
            .memoryLimitMiB(2048)
            .build());
        
        reCiterPubmedEcrRep.grantPull(reCiterPubmedTaskDefinition.obtainExecutionRole());

        ContainerDefinition reCiterPubmedNginxContainer = reCiterPubmedTaskDefinition.addContainer("reCiterPubmedNginxContainer", ContainerDefinitionProps.builder()
            .image(ContainerImage.fromRegistry("wcmcits/reciter-pubmed-nginx:latest"))
            .logging(new AwsLogDriver(AwsLogDriverProps.builder()
                    .logGroup(new LogGroup(this, "reciterNginxPubmedLogGroup", LogGroupProps.builder()
                        .logGroupName("/ecs/reciter/pubmed/nginx")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .retention(RetentionDays.ONE_MONTH)
                        .build()))
                    .streamPrefix("pubmed-logs")
                    .build()))
            .containerName("reciter-pubmed-nginx")
            .healthCheck(HealthCheck.builder()
                .command(Arrays.asList("CMD-SHELL", "curl -f http://localhost/nginx-health || exit 1"))
                .interval(Duration.minutes(5))
                .retries(2)
                .startPeriod(Duration.seconds(60))
                .timeout(Duration.seconds(30))
                .build())
            .memoryReservationMiB(400)
            .memoryLimitMiB(548)
            .taskDefinition(reCiterPubmedTaskDefinition)
            .cpu(200)
            .build());

        
        ContainerDefinition reCiterPubmedContainer = reCiterPubmedTaskDefinition.addContainer("reCiterPubmedContainer", ContainerDefinitionProps.builder()
            .image(ContainerImage.fromRegistry("wcmcits/reciter-pubmed:latest"))
            .logging(new AwsLogDriver(AwsLogDriverProps.builder()
                .logGroup(new LogGroup(this, "reciterPubmedLogGroup", LogGroupProps.builder()
                    .logGroupName("/ecs/reciter/pubmed")
                    .removalPolicy(RemovalPolicy.DESTROY)
                    .retention(RetentionDays.ONE_MONTH)
                    .build()))
                .streamPrefix("pubmed-logs")
                .build()))
            .containerName("reciter-pubmed")
            .healthCheck(HealthCheck.builder()
                .command(Arrays.asList("CMD-SHELL", "wget --spider -S http://localhost:5000/pubmed/ping || exit 1"))
                .interval(Duration.minutes(5))
                .retries(2)
                .startPeriod(Duration.seconds(60))
                .timeout(Duration.seconds(30))
                .build())
            .memoryReservationMiB(1500)
            .memoryLimitMiB(1600)
            .secrets(new HashMap<String, Secret>(){{
                put("PUBMED_API_KEY", Secret.fromSecretsManager(reciterPubmedSecret, "PUBMED_API_KEY"));
            }})
            .taskDefinition(reCiterPubmedTaskDefinition)
            .cpu(824)
            .build());

        reCiterPubmedNginxContainer.addPortMappings(PortMapping.builder()
            .containerPort(80)
            .protocol(Protocol.TCP)
            .build());

        reCiterPubmedContainer.addPortMappings(PortMapping.builder()
            .containerPort(5000)
            .protocol(Protocol.TCP)
            .build());
        
        reCiterPubmedService = new FargateService(this, "reCiterPubmedFargateService", FargateServiceProps.builder()
            .cluster(reCiterCluster)
            .taskDefinition(reCiterPubmedTaskDefinition)
            .desiredCount(1)
            .serviceName("reciter-pubmed")
            .assignPublicIp(false)
            .enableEcsManagedTags(true)
            .healthCheckGracePeriod(Duration.seconds(60))
            .platformVersion(FargatePlatformVersion.LATEST)
            .propagateTags(PropagatedTagSource.SERVICE)
            .vpcSubnets(SubnetSelection.builder()
                .onePerAz(true)
                .subnetType(SubnetType.PRIVATE)
                .build())
            .securityGroups(Arrays.asList(reciterClusterSg))
            .build());

        reCiterPubmedService.getConnections().allowFrom(reCiterEcsALB, Port.tcp(80), "Connection from ALB over port 80");
        
        if(System.getenv("INCLUDE_SCOPUS") != null && System.getenv("INCLUDE_SCOPUS").equals("true")) {
            final FargateTaskDefinition reCiterScopusTaskDefinition = new FargateTaskDefinition(this, "reCiterScopusTaskDefinition", FargateTaskDefinitionProps.builder()
                .cpu(1024)
                .memoryLimitMiB(2048)
                .build());
            
            reCiterScopusEcrRepo.grantPull(reCiterScopusTaskDefinition.obtainExecutionRole());
            
            ContainerDefinition reCiterScopusNginxContainer = reCiterScopusTaskDefinition.addContainer("reCiterScopusNginxContainer", ContainerDefinitionProps.builder()
                .image(ContainerImage.fromRegistry("wcmcits/reciter-scopus-nginx:latest"))
                .logging(new AwsLogDriver(AwsLogDriverProps.builder()
                    .logGroup(new LogGroup(this, "reciterNginxScopusLogGroup", LogGroupProps.builder()
                        .logGroupName("/ecs/reciter/scopus/nginx")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .retention(RetentionDays.ONE_MONTH)
                        .build()))
                    .streamPrefix("scopus-logs")
                    .build()))
                .containerName("reciter-scopus-nginx")
                .healthCheck(HealthCheck.builder()
                    .command(Arrays.asList("CMD-SHELL", "curl -f http://localhost/nginx-health || exit 1"))
                    .interval(Duration.minutes(5))
                    .retries(2)
                    .startPeriod(Duration.seconds(60))
                    .timeout(Duration.seconds(30))
                    .build())
                .memoryReservationMiB(400)
                .memoryLimitMiB(548)
                .taskDefinition(reCiterScopusTaskDefinition)
                .cpu(200)
                .build());
            
            ContainerDefinition reCiterScopusContainer = reCiterScopusTaskDefinition.addContainer("reCiterScopusContainer", ContainerDefinitionProps.builder()
                .image(ContainerImage.fromRegistry("wcmcits/reciter-scopus:latest"))
                .logging(new AwsLogDriver(AwsLogDriverProps.builder()
                    .logGroup(new LogGroup(this, "reciterScopusLogGroup", LogGroupProps.builder()
                        .logGroupName("/ecs/reciter/scopus")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .retention(RetentionDays.ONE_MONTH)
                        .build()))
                    .streamPrefix("scopus-logs")
                    .build()))
                .containerName("reciter-scopus")
                .healthCheck(HealthCheck.builder()
                    .command(Arrays.asList("CMD-SHELL", "wget --spider -S http://localhost:5000/scopus/ping || exit 1"))
                    .interval(Duration.minutes(5))
                    .retries(2)
                    .startPeriod(Duration.seconds(60))
                    .timeout(Duration.seconds(30))
                    .build())
                .memoryReservationMiB(1500)
                .memoryLimitMiB(1600)
                .secrets(new HashMap<String, Secret>(){{
                    put("SCOPUS_API_KEY", Secret.fromSecretsManager(reciterScopusSecret, "SCOPUS_API_KEY"));
                    put("SCOPUS_INST_TOKEN", Secret.fromSecretsManager(reciterScopusSecret, "SCOPUS_INST_TOKEN"));
                }})
                .taskDefinition(reCiterScopusTaskDefinition)
                .cpu(824)
                .build());

            reCiterScopusNginxContainer.addPortMappings(PortMapping.builder()
                .containerPort(80)
                .protocol(Protocol.TCP)
                .build());

            reCiterScopusContainer.addPortMappings(PortMapping.builder()
                .containerPort(5000)
                .protocol(Protocol.TCP)
                .build());

            

            
            reCiterScopusService = new FargateService(this, "reCiterScopusFargateService", FargateServiceProps.builder()
                .cluster(reCiterCluster)
                .taskDefinition(reCiterScopusTaskDefinition)
                .desiredCount(1)
                .serviceName("reciter-scopus")
                .assignPublicIp(false)
                .enableEcsManagedTags(true)
                .healthCheckGracePeriod(Duration.seconds(60))
                .platformVersion(FargatePlatformVersion.LATEST)
                .propagateTags(PropagatedTagSource.SERVICE)
                .vpcSubnets(SubnetSelection.builder()
                    .onePerAz(true)
                    .subnetType(SubnetType.PRIVATE)
                    .build())
                .securityGroups(Arrays.asList(reciterClusterSg))
                .build());

            reCiterScopusService.getConnections().allowFrom(reCiterEcsALB, Port.tcp(80), "Connection from ALB over port 80");
        }

        final FargateTaskDefinition reCiterPubManagerTaskDefinition = new FargateTaskDefinition(this, "reCiterPubManagerTaskDefinition", FargateTaskDefinitionProps.builder()
            .cpu(1024)
            .memoryLimitMiB(2048)
            .build());
        
        reCiterPubManagerEcrRepo.grantPull(reCiterPubManagerTaskDefinition.obtainExecutionRole());

        
        ContainerDefinition reCiterPubManagerContainer = reCiterPubManagerTaskDefinition.addContainer("reCiterScopusContainer", ContainerDefinitionProps.builder()
            .image(ContainerImage.fromRegistry("httpd:latest"))
            .logging(new AwsLogDriver(AwsLogDriverProps.builder()
                .logGroup(new LogGroup(this, "reciterPubManagerLogGroup", LogGroupProps.builder()
                    .logGroupName("/ecs/reciter/pub/manager")
                    .removalPolicy(RemovalPolicy.DESTROY)
                    .retention(RetentionDays.ONE_MONTH)
                    .build()))
                .streamPrefix("pub-manager-logs")
                .build()))
            .containerName("reciter-pub-manager")
            .healthCheck(HealthCheck.builder()
                .command(Arrays.asList("CMD-SHELL", "curl -f http://localhost:8081/login || exit 1"))
                .interval(Duration.minutes(5))
                .retries(2)
                .startPeriod(Duration.seconds(60))
                .timeout(Duration.seconds(30))
                .build())
            .memoryReservationMiB(1800)
            .memoryLimitMiB(2048)
            .taskDefinition(reCiterPubManagerTaskDefinition)
            .cpu(1024)
            .build());

        reCiterPubManagerContainer.addPortMappings(PortMapping.builder()
            .containerPort(8081)
            .protocol(Protocol.TCP)
            .build());

        
        reCiterPubManagerService = new FargateService(this, "reCiterPubManagerFargateService", FargateServiceProps.builder()
            .cluster(reCiterCluster)
            .taskDefinition(reCiterPubManagerTaskDefinition)
            .desiredCount(1)
            .serviceName("reciter-pub-manager")
            .assignPublicIp(false)
            .enableEcsManagedTags(true)
            .healthCheckGracePeriod(Duration.seconds(60))
            .platformVersion(FargatePlatformVersion.LATEST)
            .propagateTags(PropagatedTagSource.SERVICE)
            .vpcSubnets(SubnetSelection.builder()
                .onePerAz(true)
                .subnetType(SubnetType.PRIVATE)
                .build())
            .securityGroups(Arrays.asList(albSg))
            .build());

        reCiterPubManagerService.getConnections().allowFrom(reCiterEcsALB, Port.tcp(80), "Connection from ALB over port 80");

        reciterListener.addAction("reciter-redirect-to-swagger", AddApplicationActionProps.builder()
            .conditions(Arrays.asList(ListenerCondition.pathPatterns(Arrays.asList("/reciter"))))
            .action(ListenerAction.redirect(RedirectOptions.builder()
                .host("#{host}")
                .port("80")
                .path("/#{path}/swagger-ui/index.html")
                .query("#{query}")
                .permanent(true)
                .build()))
            .priority(1)
            .build());
        
        reciterListener.addAction("reciter-pubmed-redirect-to-swagger", AddApplicationActionProps.builder()
            .conditions(Arrays.asList(ListenerCondition.pathPatterns(Arrays.asList("/pubmed"))))
            .action(ListenerAction.redirect(RedirectOptions.builder()
                .host("#{host}")
                .port("80")
                .path("/#{path}/swagger-ui/index.html")
                .query("#{query}")
                .permanent(true)
                .build()))
            .priority(2)
            .build());
        if(System.getenv("INCLUDE_SCOPUS") != null && System.getenv("INCLUDE_SCOPUS").equals("true")) {
            reciterListener.addAction("reciter-scopus-redirect-to-swagger", AddApplicationActionProps.builder()
                .conditions(Arrays.asList(ListenerCondition.pathPatterns(Arrays.asList("/scopus"))))
                .action(ListenerAction.redirect(RedirectOptions.builder()
                    .host("#{host}")
                    .port("80")
                    .path("/#{path}/swagger-ui/index.html")
                    .query("#{query}")
                    .permanent(true)
                    .build()))
                .priority(3)
                .build());
        }
        //Path based redirect to reciter
        reciterListener.addTargets("reciter", AddApplicationTargetsProps.builder()
            .conditions(Arrays.asList(ListenerCondition.pathPatterns(Arrays.asList("/reciter*"))))
            .priority(4)
            .targets(Arrays.asList(reCiterService))
            .port(80)
            .targetGroupName("cdk-reciter-tg")
            .protocolVersion(ApplicationProtocolVersion.HTTP1)
            .healthCheck(software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                .enabled(true)
                .healthyThresholdCount(3)
                .interval(Duration.seconds(60))
                .path("/reciter/ping")
                .protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.HTTP)
                .unhealthyThresholdCount(3)
                .build())
            .protocol(ApplicationProtocol.HTTP)
            .build());
        //Path based redirect to reciter-pubmed
        reciterListener.addTargets("reciter-pubmed", AddApplicationTargetsProps.builder()
            .conditions(Arrays.asList(ListenerCondition.pathPatterns(Arrays.asList("/pubmed*"))))
            .priority(5)
            .targets(Arrays.asList(reCiterPubmedService))
            .port(80)
            .targetGroupName("cdk-reciter-pubmed-tg")
            .protocolVersion(ApplicationProtocolVersion.HTTP1)
            .healthCheck(software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                .enabled(true)
                .healthyThresholdCount(3)
                .interval(Duration.seconds(60))
                .path("/pubmed/ping")
                .protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.HTTP)
                .unhealthyThresholdCount(3)
                .build())
            .protocol(ApplicationProtocol.HTTP)
            .build());
        if(System.getenv("INCLUDE_SCOPUS") != null && System.getenv("INCLUDE_SCOPUS").equals("true")) {
            //Path based redirect to reciter-scopus
            reciterListener.addTargets("reciter-scopus", AddApplicationTargetsProps.builder()
                .conditions(Arrays.asList(ListenerCondition.pathPatterns(Arrays.asList("/scopus*"))))
                .priority(6)
                .targets(Arrays.asList(reCiterScopusService))
                .port(80)
                .targetGroupName("cdk-reciter-scopus-tg")
                .protocolVersion(ApplicationProtocolVersion.HTTP1)
                .healthCheck(software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                    .enabled(true)
                    .healthyThresholdCount(3)
                    .interval(Duration.seconds(60))
                    .path("/scopus/ping")
                    .protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.HTTP)
                    .unhealthyThresholdCount(3)
                    .build())
                .protocol(ApplicationProtocol.HTTP)
                .build());
        }
        //Path based redirect to reciter-pub-manager
        reciterListener.addTargets("reciter-pub-manager", AddApplicationTargetsProps.builder()
            .conditions(Arrays.asList(ListenerCondition.pathPatterns(Arrays.asList("/*"))))
            .priority(7)
            .targets(Arrays.asList(reCiterPubManagerService))
            .port(80)
            .targetGroupName("cdk-reciter-pub-manager-tg")
            .protocolVersion(ApplicationProtocolVersion.HTTP1)
            .healthCheck(software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                .enabled(true)
                .healthyThresholdCount(3)
                .interval(Duration.seconds(60))
                .path("/login")
                .protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.HTTP)
                .unhealthyThresholdCount(3)
                .build())
            .protocol(ApplicationProtocol.HTTP)
            .build());
        
        ScalableTaskCount reCiterAutoScaling =  reCiterService.autoScaleTaskCount(EnableScalingProps.builder()
            .minCapacity(1)
            .maxCapacity(2)
            .build());
        
        reCiterAutoScaling.scaleOnCpuUtilization("reciterScaleOnCPUUtilization", CpuUtilizationScalingProps.builder()
            .targetUtilizationPercent(85)
            .scaleInCooldown(Duration.minutes(10))
            .build());
        
        reCiterAutoScaling.scaleOnMemoryUtilization("reciterScaleOnMemoryUtilization", MemoryUtilizationScalingProps.builder()
            .targetUtilizationPercent(85)
            .scaleInCooldown(Duration.minutes(10))
            .build());
        
        ScalableTaskCount reCiterPubmedAutoScaling =  reCiterPubmedService.autoScaleTaskCount(EnableScalingProps.builder()
            .minCapacity(1)
            .maxCapacity(2)
            .build());

        reCiterPubmedAutoScaling.scaleOnCpuUtilization("reciterPubmedScaleOnCPUUtilization", CpuUtilizationScalingProps.builder()
            .targetUtilizationPercent(85)
            .scaleInCooldown(Duration.minutes(10))
            .build());
        
        reCiterPubmedAutoScaling.scaleOnMemoryUtilization("reciterPubmedScaleOnMemoryUtilization", MemoryUtilizationScalingProps.builder()
            .targetUtilizationPercent(85)
            .scaleInCooldown(Duration.minutes(10))
            .build());
        if(System.getenv("INCLUDE_SCOPUS") != null && System.getenv("INCLUDE_SCOPUS").equals("true")) {
            ScalableTaskCount reCiterScopusAutoScaling =  reCiterScopusService.autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(1)
                .maxCapacity(2)
                .build());

            reCiterScopusAutoScaling.scaleOnCpuUtilization("reciterScopusScaleOnCPUUtilization", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(85)
                .scaleInCooldown(Duration.minutes(10))
                .build());
            
            reCiterScopusAutoScaling.scaleOnMemoryUtilization("reciterScopusScaleOnMemoryUtilization", MemoryUtilizationScalingProps.builder()
                .targetUtilizationPercent(85)
                .scaleInCooldown(Duration.minutes(10))
                .build());
        }
        
        ScalableTaskCount reCiterPubManagerAutoScaling =  reCiterPubManagerService.autoScaleTaskCount(EnableScalingProps.builder()
            .minCapacity(1)
            .maxCapacity(2)
            .build());

        reCiterPubManagerAutoScaling.scaleOnCpuUtilization("reciterPubManagerScaleOnCPUUtilization", CpuUtilizationScalingProps.builder()
            .targetUtilizationPercent(85)
            .scaleInCooldown(Duration.minutes(10))
            .build());
        
        reCiterPubManagerAutoScaling.scaleOnMemoryUtilization("reciterPubManagerScaleOnMemoryUtilization", MemoryUtilizationScalingProps.builder()
            .targetUtilizationPercent(85)
            .scaleInCooldown(Duration.minutes(10))
            .build());

        //Create a SNS Topic
        reciterAlarmTopic = new Topic(this, "reCiterSnsTopic", TopicProps.builder()
            .topicName("reciter-monitor-topic")
            .displayName("reciter-monitor-topic")
            .build());
        
        new Subscription(this, "emailSub", SubscriptionProps.builder()
            .protocol(SubscriptionProtocol.EMAIL)
            .region(this.getRegion())
            .topic(reciterAlarmTopic)
            .endpoint(System.getenv("ALARM_EMAIL"))
            .build());

        //Alarms for app and Topic to send to email sub
        final Alarm reciterHighCpuUtilAlarm = new Alarm(this, "reciterHighCpuUtilAlarm", AlarmProps.builder()
            .alarmName("cdk-reciter-cpu-high")
            .alarmDescription("This alarm is for high cpu utilization for ReCiter")
            .metric(reCiterService.metricCpuUtilization())
            .datapointsToAlarm(1)
            .evaluationPeriods(1)
            .threshold(85)
            .treatMissingData(TreatMissingData.MISSING)
            .build());
        
        reciterHighCpuUtilAlarm.addAlarmAction(new SnsAction(reciterAlarmTopic));
        
        final Alarm reciterHighMemoryUtilAlarm = new Alarm(this, "cbtApiHighMemoryUtilAlarm", AlarmProps.builder()
            .alarmName("cdk-reciter-memory-high")
            .alarmDescription("This alarm is for high memory utilization for ReCiter")
            .metric(reCiterService.metricMemoryUtilization())
            .datapointsToAlarm(1)
            .evaluationPeriods(1)
            .threshold(85)
            .treatMissingData(TreatMissingData.MISSING)
            .build());
        
        reciterHighMemoryUtilAlarm.addAlarmAction(new SnsAction(reciterAlarmTopic));

        final Alarm reciterPubmedHighCpuUtilAlarm = new Alarm(this, "reciterPubmedHighCpuUtilAlarm", AlarmProps.builder()
            .alarmName("cdk-reciter-pubmed-cpu-high")
            .alarmDescription("This alarm is for high cpu utilization for ReCiter-Pubmed")
            .metric(reCiterPubmedService.metricCpuUtilization())
            .datapointsToAlarm(1)
            .evaluationPeriods(1)
            .threshold(85)
            .treatMissingData(TreatMissingData.MISSING)
            .build());
        
        reciterPubmedHighCpuUtilAlarm.addAlarmAction(new SnsAction(reciterAlarmTopic));
        
        final Alarm reciterPubmedHighMemoryUtilAlarm = new Alarm(this, "reciterPubmedHighMemoryUtilAlarm", AlarmProps.builder()
            .alarmName("cdk-reciter-pubmed-memory-high")
            .alarmDescription("This alarm is for high memory utilization for ReCiter-Pubmed")
            .metric(reCiterPubmedService.metricMemoryUtilization())
            .datapointsToAlarm(1)
            .evaluationPeriods(1)
            .threshold(85)
            .treatMissingData(TreatMissingData.MISSING)
            .build());
        
        reciterPubmedHighMemoryUtilAlarm.addAlarmAction(new SnsAction(reciterAlarmTopic));
        if(System.getenv("INCLUDE_SCOPUS") != null && System.getenv("INCLUDE_SCOPUS").equals("true")) {
            final Alarm reciterScopusHighCpuUtilAlarm = new Alarm(this, "reciterScopusHighCpuUtilAlarm", AlarmProps.builder()
                .alarmName("cdk-reciter-scopus-cpu-high")
                .alarmDescription("This alarm is for high cpu utilization for ReCiter-Scopus")
                .metric(reCiterScopusService.metricCpuUtilization())
                .datapointsToAlarm(1)
                .evaluationPeriods(1)
                .threshold(85)
                .treatMissingData(TreatMissingData.MISSING)
                .build());
            
            reciterScopusHighCpuUtilAlarm.addAlarmAction(new SnsAction(reciterAlarmTopic));
            
            final Alarm reciterScopusHighMemoryUtilAlarm = new Alarm(this, "reciterScopusHighMemoryUtilAlarm", AlarmProps.builder()
                .alarmName("cdk-reciter-scopus-memory-high")
                .alarmDescription("This alarm is for high memory utilization for ReCiter-Scopus")
                .metric(reCiterScopusService.metricMemoryUtilization())
                .datapointsToAlarm(1)
                .evaluationPeriods(1)
                .threshold(85)
                .treatMissingData(TreatMissingData.MISSING)
                .build());
            
            reciterScopusHighMemoryUtilAlarm.addAlarmAction(new SnsAction(reciterAlarmTopic));
        }

        final Alarm reciterPubManagerHighCpuUtilAlarm = new Alarm(this, "reciterPubManagerHighCpuUtilAlarm", AlarmProps.builder()
            .alarmName("cdk-reciter-pub-manager-cpu-high")
            .alarmDescription("This alarm is for high cpu utilization for ReCiter-Pub-Manager")
            .metric(reCiterPubManagerService.metricCpuUtilization())
            .datapointsToAlarm(1)
            .evaluationPeriods(1)
            .threshold(85)
            .treatMissingData(TreatMissingData.MISSING)
            .build());
        
        reciterPubManagerHighCpuUtilAlarm.addAlarmAction(new SnsAction(reciterAlarmTopic));
        
        final Alarm reciterPubManagerHighMemoryUtilAlarm = new Alarm(this, "reciterPubManagerHighMemoryUtilAlarm", AlarmProps.builder()
            .alarmName("cdk-reciter-pub-manager-memory-high")
            .alarmDescription("This alarm is for high memory utilization for ReCiter-Pub-Manager")
            .metric(reCiterPubManagerService.metricMemoryUtilization())
            .datapointsToAlarm(1)
            .evaluationPeriods(1)
            .threshold(85)
            .treatMissingData(TreatMissingData.MISSING)
            .build());
        
        reciterPubManagerHighMemoryUtilAlarm.addAlarmAction(new SnsAction(reciterAlarmTopic));
        
        //Tagging for all Resources
        Tags.of(this).add("application", "reciter");
        Tags.of(this).add("stack-id", ReCiterCDKECSStack.of(this).getStackId());
        Tags.of(this).add("stack-name", ReCiterCDKECSStack.of(this).getStackName());
        Tags.of(this).add("stack-region", ReCiterCDKECSStack.of(this).getRegion());

        CfnOutput.Builder.create(this, "reciterUrl")
            .description("ReCiter URL")
            .exportName("reciterUrl")
            .value(reCiterEcsALB.getLoadBalancerDnsName() + "/reciter")
            .build();

        CfnOutput.Builder.create(this, "reciterPubmedUrl")
            .description("ReCiter Pubmed URL")
            .exportName("reciterPubmedUrl")
            .value(reCiterEcsALB.getLoadBalancerDnsName() + "/pubmed")
            .build();
        if(System.getenv("INCLUDE_SCOPUS") != null && System.getenv("INCLUDE_SCOPUS").equals("true")) {
            CfnOutput.Builder.create(this, "reciterScopusUrl")
                .description("ReCiter Scopus URL")
                .exportName("reciterScopusUrl")
                .value(reCiterEcsALB.getLoadBalancerDnsName() + "/scopus")
                .build();
        }

        CfnOutput.Builder.create(this, "reciterPubManagerUrl")
            .description("ReCiter Pub Manager URL")
            .exportName("reciterPubManagerUrl")
            .value(reCiterEcsALB.getLoadBalancerDnsName() + "/login")
            .build();
    }

    public ApplicationLoadBalancer getAlb() {
        return this.reCiterEcsALB;
    }

    public Cluster getCluster() {
        return this.reCiterCluster;
    }

    public Topic getReCiterTopic() {
        return this.reciterAlarmTopic;
    }

    public FargateService getReCiterPubmedService() {
        return this.reCiterPubmedService;
    }

    public FargateService getReCiterScopusService() {
        return this.reCiterScopusService;
    }

    public FargateService getReCiterService() {
        return this.reCiterService;
    }

    public FargateService getReCiterPubManagerService() {
        return this.reCiterPubManagerService;
    }

}
