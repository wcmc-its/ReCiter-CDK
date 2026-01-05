package edu.wcm.reciter;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.NestedStack;
import software.amazon.awscdk.NestedStackProps;
import software.amazon.awscdk.services.codedeploy.LambdaDeploymentConfig;
import software.amazon.awscdk.services.codedeploy.LambdaDeploymentGroup;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.lambda.DockerImageCode;
import software.amazon.awscdk.services.lambda.DockerImageFunction;
import software.amazon.awscdk.services.lambda.IFunction;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.RuntimeFamily;
import software.amazon.awscdk.services.lambda.nodejs.NodejsFunction;
import software.amazon.awscdk.services.rds.SubnetGroup;
import software.constructs.Construct;

public class ReCiterCdkLambdaStack extends NestedStack {

	 private IFunction notifierLambdaFunction;
	 private final IFunction scoringLambdaFunction;
	 
	 public ReCiterCdkLambdaStack(final Construct parent, final String id, final NestedStackProps props, IVpc vpc, SubnetGroup privateDbSubnetGroup, SubnetGroup publicDbSubnetGroup) {
	        super(parent, id, props);

	   // Manually define Node.js 20 since it's missing in v1
	     Runtime nodeJs20 = new Runtime("nodejs20.x", RuntimeFamily.NODEJS);   
  
	   // 1. Define the NodejsFunction (it auto-bundles your index.ts)
        notifierLambdaFunction = NodejsFunction.Builder.create(this, "ReCiterPubNotifier")
            .runtime(nodeJs20)
            .entry("src/lambda/index.ts")
            .handler("handler")
            .vpc(vpc)
            .build();
    
    //  2. Use .getCurrentVersion() to ensure CDK tracks code changes
     Alias liveAlias = Alias.Builder.create(this, "LiveAlias")
         .aliasName("Live")
         .version(notifierLambdaFunction.getLatestVersion())
         .build();

     // 3. Setup the Rolling Update (Canary) via CodeDeploy
     LambdaDeploymentGroup.Builder.create(this, "CanaryDeployment")
         .alias(liveAlias)
         // Shift 10% of traffic every 5 minutes until 100% is reached
         .deploymentConfig(LambdaDeploymentConfig.CANARY_10_PERCENT_5_MINUTES)
         .build();
                
 	 
	// 2. Reference your ECR repository
     IRepository repository = Repository.fromRepositoryName(this, "ScoringRepo", "wcmc/reciter/reciter-scoring");

     // 3. Initialize the scoring function
     this.scoringLambdaFunction = DockerImageFunction.Builder.create(this, "ReCiterScoring")
         .functionName("reciter-scoring-prod")
         .code(DockerImageCode.fromEcr(repository)) // Uses 'latest' by default
         .vpc(vpc)
         .vpcSubnets(SubnetSelection.builder().subnetGroupName("reciter-private-app-subnet").build())
         .memorySize(1232)
         .timeout(Duration.minutes(3))
         .build();
	 }
	 
	 
	 public IFunction getReCiterNotifierLambdaFunction() {
	        return this.notifierLambdaFunction;
	 }
	 
	 public IFunction getReCiterScoringLambdaFunction() {
	        return this.scoringLambdaFunction;
	    }
	
   }

