package edu.wcm.reciter;

import java.util.Arrays;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.NestedStack;
import software.amazon.awscdk.core.NestedStackProps;
import software.amazon.awscdk.core.SecretValue;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariableType;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.Cache;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.GitHubSourceProps;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.LocalCacheMode;
import software.amazon.awscdk.services.codebuild.Project;
import software.amazon.awscdk.services.codebuild.ProjectProps;
import software.amazon.awscdk.services.codebuild.Source;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.ArtifactPath;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.PipelineProps;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildActionProps;
import software.amazon.awscdk.services.codepipeline.actions.EcsDeployAction;
import software.amazon.awscdk.services.codepipeline.actions.EcsDeployActionProps;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceActionProps;
import software.amazon.awscdk.services.codepipeline.actions.ManualApprovalAction;
import software.amazon.awscdk.services.codepipeline.actions.ManualApprovalActionProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.sns.Topic;

public class ReCiterCdkPipelineStack extends NestedStack {

    private final Project reCiterPubmedCodebuildProject;
    private Project reCiterScopusCodebuildProject;
    private final Project reCiterCodebuildProject;
    private final Project reCiterPubManagerCodebuildProject;
    private final Project reCiterMachineLearningAnalysisProject;
    private Artifact sourceOutput = new Artifact();
    private Artifact buildOutput = new Artifact();
    private Artifact reCiterScopusSourceOutput = new Artifact();
    private Artifact reCiterScopusbuildOutput = new Artifact();
    private Artifact reCiterSourceOutput = new Artifact();
    private Artifact reCiterbuildOutput = new Artifact();
    private Artifact reCiterPubManagerSourceOutput = new Artifact();
    private Artifact reCiterPubManagerbuildOutput = new Artifact();
    private Artifact reCiterMachineLearningAnalysisSourceOutput = new Artifact();
    private Artifact reCiterMachineLearningAnalysisbuildOutput = new Artifact();
    
    public ReCiterCdkPipelineStack(final Construct parent, final String id) {
        this(parent, id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public ReCiterCdkPipelineStack(final Construct parent, final String id, final NestedStackProps props, Cluster reCiterCluster, Repository reCiterEcrRepo, Repository reCiterPubmedEcrRepo, Repository reCiterScopusEcrRepo, Repository reCiterPubManagerEcrRepo, Repository reCiterMachineLearningAnalysisRepo, Topic reCiterTopic, 
    FargateService reCiterPubmedService, FargateService reCiterScopusService, FargateService reCiterService, FargateService reCiterPubManagerService,
    ISecret reCiterSecret, ApplicationLoadBalancer reCiterAlb, IVpc vpc) {
        super(parent, id, props);

        //CodeBuild Project for ReCiter-Pubmed
        reCiterPubmedCodebuildProject = new Project(this, "reciter-pubmed-codebuild", ProjectProps.builder()
            .projectName("ReCiter-Pubmed-Retrieval-Tool")
            .source(Source.gitHub(GitHubSourceProps.builder()
                .owner(System.getenv("GITHUB_USER"))
                .repo("ReCiter-PubMed-Retrieval-Tool")
                .webhook(false)
                //.webhookFilters(Arrays.asList(FilterGroup.inEventOf(EventAction.PUSH).andBranchIs("master")))
                .reportBuildStatus(false)
                .build()))
            .badge(true)
            .cache(Cache.local(LocalCacheMode.DOCKER_LAYER))
            .vpc(vpc)
            .subnetSelection(SubnetSelection.builder()
                .onePerAz(true)
                .subnetType(SubnetType.PRIVATE)
                .build())
            .environment(BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.AMAZON_LINUX_2_3)
                .computeType(ComputeType.SMALL)
                .privileged(true)
                .build())
            .environmentVariables(new HashMap<String, BuildEnvironmentVariable>(){{
                put("ECR_REPO_URI", BuildEnvironmentVariable.builder()
                        .type(BuildEnvironmentVariableType.PLAINTEXT)
                        .value(reCiterPubmedEcrRepo.getRepositoryUri())
                        .build());
                put("CLUSTER_NAME", BuildEnvironmentVariable.builder()
                    .type(BuildEnvironmentVariableType.PLAINTEXT)
                    .value(reCiterCluster.getClusterName())
                    .build());
            }})
            .buildSpec(BuildSpec.fromObjectToYaml(new HashMap<String, Object>(){{
                put("version", "0.2");
                put("phases", new JSONObject()
                    .put("pre_build", new JSONObject()
                        .put("commands", new JSONArray()
                            .put("env")
                            .put("export TAG=${CODEBUILD_RESOLVED_SOURCE_VERSION}")))
                    .put("build", new JSONObject()
                        .put("commands", new JSONArray()
                            .put("mvn clean install -Dmaven.test.skip=true")
                            .put("docker build -t $ECR_REPO_URI:$TAG -t $ECR_REPO_URI:latest .")
                            .put("$(aws ecr get-login --no-include-email)")
                            .put("docker push $ECR_REPO_URI:$TAG")))
                    .put("post_build", new JSONObject()
                        .put("commands", new JSONArray()
                            .put("echo \"In Post-Build Stage\"")
                            .put("printf '[{\"name\":\"reciter-pubmed\",\"imageUri\":\"%s\"}]' $ECR_REPO_URI:$TAG > imagedefinitions.json")
                            .put("pwd; ls -al; cat imagedefinitions.json"))).toMap());
                put("artifacts", new JSONObject()
                .put("files", new JSONArray()
                    .put("imagedefinitions.json")).toMap());
            }}))
            .build());

        //Roles section
        reCiterPubmedCodebuildProject.addToRolePolicy(PolicyStatement.Builder.create()
            .actions(Arrays.asList("ecs:DescribeCluster",
            "ecr:GetAuthorizationToken",
            "ecr:BatchCheckLayerAvailability",
            "ecr:BatchGetImage",
            "ecr:GetDownloadUrlForLayer"))
            .resources(Arrays.asList(reCiterCluster.getClusterArn()))
            .build());
        reCiterPubmedEcrRepo.grantPullPush(reCiterPubmedCodebuildProject.getRole());

        if(System.getenv("INCLUDE_SCOPUS") != null && System.getenv("INCLUDE_SCOPUS").equals("true")) {
            //CodeBuild Project for ReCiter-Scopus
            reCiterScopusCodebuildProject = new Project(this, "reciter-scopus-codebuild", ProjectProps.builder()
                .projectName("ReCiter-Scopus-Retrieval-Tool")
                .source(Source.gitHub(GitHubSourceProps.builder()
                    .owner(System.getenv("GITHUB_USER"))
                    .repo("ReCiter-Scopus-Retrieval-Tool")
                    .webhook(false)
                    //.webhookFilters(Arrays.asList(FilterGroup.inEventOf(EventAction.PUSH).andBranchIs("master")))
                    .reportBuildStatus(false)
                    .build()))
                .environment(BuildEnvironment.builder()
                    .buildImage(LinuxBuildImage.AMAZON_LINUX_2_3)
                    .computeType(ComputeType.SMALL)
                    .privileged(true)
                    .build())
                .badge(true)
                .cache(Cache.local(LocalCacheMode.DOCKER_LAYER))
                .vpc(vpc)
                .subnetSelection(SubnetSelection.builder()
                    .onePerAz(true)
                    .subnetType(SubnetType.PRIVATE)
                    .build())
                .environmentVariables(new HashMap<String, BuildEnvironmentVariable>(){{
                    put("ECR_REPO_URI", BuildEnvironmentVariable.builder()
                            .type(BuildEnvironmentVariableType.PLAINTEXT)
                            .value(reCiterScopusEcrRepo.getRepositoryUri())
                            .build());
                    put("CLUSTER_NAME", BuildEnvironmentVariable.builder()
                        .type(BuildEnvironmentVariableType.PLAINTEXT)
                        .value(reCiterCluster.getClusterName())
                        .build());
                }})
                .buildSpec(BuildSpec.fromObjectToYaml(new HashMap<String, Object>(){{
                    put("version", "0.2");
                    put("phases", new JSONObject()
                        .put("pre_build", new JSONObject()
                            .put("commands", new JSONArray()
                                .put("env")
                                .put("export TAG=${CODEBUILD_RESOLVED_SOURCE_VERSION}")))
                        .put("build", new JSONObject()
                            .put("commands", new JSONArray()
                                .put("mvn clean install -Dmaven.test.skip=true")
                                .put("docker build -t $ECR_REPO_URI:$TAG -t $ECR_REPO_URI:latest .")
                                .put("$(aws ecr get-login --no-include-email)")
                                .put("docker push $ECR_REPO_URI:$TAG")))
                        .put("post_build", new JSONObject()
                            .put("commands", new JSONArray()
                                .put("echo \"In Post-Build Stage\"")
                                .put("printf '[{\"name\":\"reciter-scopus\",\"imageUri\":\"%s\"}]' $ECR_REPO_URI:$TAG > imagedefinitions.json")
                                .put("pwd; ls -al; cat imagedefinitions.json"))).toMap());
                    put("artifacts", new JSONObject()
                    .put("files", new JSONArray()
                        .put("imagedefinitions.json")).toMap());
                }}))
                .build());

            //Roles section
            reCiterScopusCodebuildProject.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(Arrays.asList("ecs:DescribeCluster",
                "ecr:GetAuthorizationToken",
                "ecr:BatchCheckLayerAvailability",
                "ecr:BatchGetImage",
                "ecr:GetDownloadUrlForLayer"))
                .resources(Arrays.asList(reCiterCluster.getClusterArn()))
                .build());
            reCiterScopusEcrRepo.grantPullPush(reCiterScopusCodebuildProject.getRole());
        }

        //CodeBuild Project for ReCiter
        reCiterCodebuildProject = new Project(this, "reciter-codebuild", ProjectProps.builder()
            .projectName("ReCiter")
            .source(Source.gitHub(GitHubSourceProps.builder()
                .owner(System.getenv("GITHUB_USER"))
                .repo("ReCiter")
                .webhook(false)
                //.webhookFilters(Arrays.asList(FilterGroup.inEventOf(EventAction.PUSH).andBranchIs("master")))
                .reportBuildStatus(false)
                .build()))
            .badge(true)
            .cache(Cache.local(LocalCacheMode.DOCKER_LAYER))
            .vpc(vpc)
                .subnetSelection(SubnetSelection.builder()
                    .onePerAz(true)
                    .subnetType(SubnetType.PRIVATE)
                    .build())
            .environment(BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.AMAZON_LINUX_2_3)
                .computeType(ComputeType.SMALL)
                .privileged(true)
                .build())
            .environmentVariables(new HashMap<String, BuildEnvironmentVariable>(){{
                put("ECR_REPO_URI", BuildEnvironmentVariable.builder()
                        .type(BuildEnvironmentVariableType.PLAINTEXT)
                        .value(reCiterEcrRepo.getRepositoryUri())
                        .build());
                put("CLUSTER_NAME", BuildEnvironmentVariable.builder()
                    .type(BuildEnvironmentVariableType.PLAINTEXT)
                    .value(reCiterCluster.getClusterName())
                    .build());
            }})
            .buildSpec(BuildSpec.fromObjectToYaml(new HashMap<String, Object>(){{
                put("version", "0.2");
                put("phases", new JSONObject()
                    .put("pre_build", new JSONObject()
                        .put("commands", new JSONArray()
                            .put("env")
                            .put("export TAG=${CODEBUILD_RESOLVED_SOURCE_VERSION}")))
                    .put("build", new JSONObject()
                        .put("commands", new JSONArray()
                            .put("mvn clean install -Dmaven.test.skip=true")
                            .put("docker build -t $ECR_REPO_URI:$TAG -t $ECR_REPO_URI:latest .")
                            .put("$(aws ecr get-login --no-include-email)")
                            .put("docker push $ECR_REPO_URI:$TAG")))
                    .put("post_build", new JSONObject()
                        .put("commands", new JSONArray()
                            .put("echo \"In Post-Build Stage\"")
                            .put("printf '[{\"name\":\"reciter\",\"imageUri\":\"%s\"}]' $ECR_REPO_URI:$TAG > imagedefinitions.json")
                            .put("pwd; ls -al; cat imagedefinitions.json"))).toMap());
                put("artifacts", new JSONObject()
                .put("files", new JSONArray()
                    .put("imagedefinitions.json")).toMap());
            }}))
            .build());

        //Roles section
        reCiterCodebuildProject.addToRolePolicy(PolicyStatement.Builder.create()
            .actions(Arrays.asList("ecs:DescribeCluster",
            "ecr:GetAuthorizationToken",
            "ecr:BatchCheckLayerAvailability",
            "ecr:BatchGetImage",
            "ecr:GetDownloadUrlForLayer"))
            .resources(Arrays.asList(reCiterCluster.getClusterArn()))
            .build());
        reCiterEcrRepo.grantPullPush(reCiterCodebuildProject.getRole());

        //CodeBuild Project for ReCiter-Pub-Manager
        reCiterPubManagerCodebuildProject = new Project(this, "reciter-pub-manager-codebuild", ProjectProps.builder()
            .projectName("ReCiter-Publication-Manager")
            .source(Source.gitHub(GitHubSourceProps.builder()
                .owner(System.getenv("GITHUB_USER"))
                .repo("ReCiter-Publication-Manager")
                .webhook(false)
                //.webhookFilters(Arrays.asList(FilterGroup.inEventOf(EventAction.PUSH).andBranchIs("master")))
                .reportBuildStatus(false)
                .build()))
            .badge(true)
            .cache(Cache.local(LocalCacheMode.DOCKER_LAYER))
            .vpc(vpc)
                .subnetSelection(SubnetSelection.builder()
                    .onePerAz(true)
                    .subnetType(SubnetType.PRIVATE)
                    .build())
            .environment(BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.AMAZON_LINUX_2_3)
                .computeType(ComputeType.SMALL)
                .privileged(true)
                .build())
            .environmentVariables(new HashMap<String, BuildEnvironmentVariable>(){{
                put("ECR_REPO_URI", BuildEnvironmentVariable.builder()
                        .type(BuildEnvironmentVariableType.PLAINTEXT)
                        .value(reCiterPubManagerEcrRepo.getRepositoryUri())
                        .build());
                put("CLUSTER_NAME", BuildEnvironmentVariable.builder()
                    .type(BuildEnvironmentVariableType.PLAINTEXT)
                    .value(reCiterCluster.getClusterName())
                    .build());
                put("ADMIN_API_KEY", BuildEnvironmentVariable.builder()
                    .type(BuildEnvironmentVariableType.SECRETS_MANAGER)
                    .value(reCiterSecret.getSecretArn() + ":ADMIN_API_KEY")
                    .build());
                put("RECITER_ALB_URL", BuildEnvironmentVariable.builder()
                    .type(BuildEnvironmentVariableType.PLAINTEXT)
                    .value(reCiterAlb.getLoadBalancerDnsName())
                    .build());
                put("TOKEN_SECRET", BuildEnvironmentVariable.builder()
                    .type(BuildEnvironmentVariableType.PLAINTEXT)
                    .value("d9mpGQUKzgq*7A#X")
                    .build());
            }})
            .buildSpec(BuildSpec.fromObjectToYaml(new HashMap<String, Object>(){{
                put("version", "0.2");
                put("phases", new JSONObject()
                    .put("pre_build", new JSONObject()
                        .put("commands", new JSONArray()
                            .put("env")
                            .put("export TAG=${CODEBUILD_RESOLVED_SOURCE_VERSION}")))
                    .put("build", new JSONObject()
                        .put("commands", new JSONArray()
                            .put("sed -i -e \"s/ADMIN_API_KEY/$ADMIN_API_KEY/g\" config/local.js")
                            .put("sed -i -e \"s/TOKEN_SECRET/$TOKEN_SECRET/g\" config/local.js")
                            .put("sed -i -e \"s/RECITER_ALB_URL/$RECITER_ALB_URL/g\" config/local.js")
                            .put("cat config/local.js")
                            .put("docker build -t $ECR_REPO_URI:$TAG -t $ECR_REPO_URI:latest .")
                            .put("$(aws ecr get-login --no-include-email)")
                            .put("docker push $ECR_REPO_URI:$TAG")))
                    .put("post_build", new JSONObject()
                        .put("commands", new JSONArray()
                            .put("echo \"In Post-Build Stage\"")
                            .put("printf '[{\"name\":\"reciter-pub-manager\",\"imageUri\":\"%s\"}]' $ECR_REPO_URI:$TAG > imagedefinitions.json")
                            .put("pwd; ls -al; cat imagedefinitions.json"))).toMap());
                put("artifacts", new JSONObject()
                .put("files", new JSONArray()
                    .put("imagedefinitions.json")).toMap());
            }}))
            .build());

        //Roles section
        reCiterPubManagerCodebuildProject.addToRolePolicy(PolicyStatement.Builder.create()
            .actions(Arrays.asList("ecs:DescribeCluster",
            "ecr:GetAuthorizationToken",
            "ecr:BatchCheckLayerAvailability",
            "ecr:BatchGetImage",
            "ecr:GetDownloadUrlForLayer"))
            .resources(Arrays.asList(reCiterCluster.getClusterArn()))
            .build());
        reCiterPubManagerEcrRepo.grantPullPush(reCiterPubManagerCodebuildProject.getRole());

        //CodeBuild Project for ReCiter-Machine-Learning-Analysis
        reCiterMachineLearningAnalysisProject = new Project(this, "reciter-machine-learning-analysis-codebuild", ProjectProps.builder()
            .projectName("ReCiter-Machine-Learning-Analysis")
            .source(Source.gitHub(GitHubSourceProps.builder()
                .owner(System.getenv("GITHUB_USER"))
                .repo("ReCiter-MachineLearning-Analysis")
                .webhook(false)
                //.webhookFilters(Arrays.asList(FilterGroup.inEventOf(EventAction.PUSH).andBranchIs("master")))
                .reportBuildStatus(false)
                .build()))
            .badge(true)
            .cache(Cache.local(LocalCacheMode.DOCKER_LAYER))
            .vpc(vpc)
                .subnetSelection(SubnetSelection.builder()
                    .onePerAz(true)
                    .subnetType(SubnetType.PRIVATE)
                    .build())
            .environment(BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.AMAZON_LINUX_2_3)
                .computeType(ComputeType.SMALL)
                .privileged(true)
                .build())
            .environmentVariables(new HashMap<String, BuildEnvironmentVariable>(){{
                put("ECR_REPO_URI", BuildEnvironmentVariable.builder()
                        .type(BuildEnvironmentVariableType.PLAINTEXT)
                        .value(reCiterMachineLearningAnalysisRepo.getRepositoryUri())
                        .build());
                put("CLUSTER_NAME", BuildEnvironmentVariable.builder()
                    .type(BuildEnvironmentVariableType.PLAINTEXT)
                    .value(reCiterCluster.getClusterName())
                    .build());
            }})
            .buildSpec(BuildSpec.fromObjectToYaml(new HashMap<String, Object>(){{
                put("version", "0.2");
                put("phases", new JSONObject()
                    .put("pre_build", new JSONObject()
                        .put("commands", new JSONArray()
                            .put("env")
                            .put("export TAG=${CODEBUILD_RESOLVED_SOURCE_VERSION}")))
                    .put("build", new JSONObject()
                        .put("commands", new JSONArray()
                            .put("docker build -t $ECR_REPO_URI:$TAG -t $ECR_REPO_URI:latest .")
                            .put("$(aws ecr get-login --no-include-email)")
                            .put("docker push $ECR_REPO_URI:$TAG")))
                    .put("post_build", new JSONObject()
                        .put("commands", new JSONArray()
                            .put("echo \"In Post-Build Stage\"")
                            .put("printf '[{\"name\":\"reciter-machine-learning-analysis\",\"imageUri\":\"%s\"}]' $ECR_REPO_URI:$TAG > imagedefinitions.json")
                            .put("pwd; ls -al; cat imagedefinitions.json"))).toMap());
                put("artifacts", new JSONObject()
                .put("files", new JSONArray()
                    .put("imagedefinitions.json")).toMap());
            }}))
            .build());

        //Roles section
        reCiterMachineLearningAnalysisProject.addToRolePolicy(PolicyStatement.Builder.create()
            .actions(Arrays.asList("ecs:DescribeCluster",
            "ecr:GetAuthorizationToken",
            "ecr:BatchCheckLayerAvailability",
            "ecr:BatchGetImage",
            "ecr:GetDownloadUrlForLayer"))
            .resources(Arrays.asList(reCiterCluster.getClusterArn()))
            .build());
        reCiterMachineLearningAnalysisRepo.grantPullPush(reCiterMachineLearningAnalysisProject.getRole());


        //ReCiter-Pubmed Pipeline
        final GitHubSourceAction sourceAction = new GitHubSourceAction(GitHubSourceActionProps.builder()
            .actionName("Github_Source")
            .owner(System.getenv("GITHUB_USER"))
            .repo("ReCiter-PubMed-Retrieval-Tool")
            .branch("master")
            .oauthToken(SecretValue.plainText(System.getenv("GITHUB_PERSONAL_ACCESS_TOKEN")))
            .output(sourceOutput)
            .build());

        final CodeBuildAction buildAction = new CodeBuildAction(CodeBuildActionProps.builder()
            .actionName("CodeBuild")
            .project(reCiterPubmedCodebuildProject)
            .input(sourceOutput)
            .outputs(Arrays.asList(buildOutput))
            .build());

        final ManualApprovalAction manualApprovalAction = new ManualApprovalAction(ManualApprovalActionProps.builder()
            .actionName("Approve")
            .notificationTopic(reCiterTopic)
            .build());

        final EcsDeployAction deployAction = new EcsDeployAction(EcsDeployActionProps.builder()
            .actionName("DeployAction")
            .service(reCiterPubmedService)
            .imageFile(new ArtifactPath(buildOutput, "imagedefinitions.json"))
            .build());

        final Pipeline reCiterPubmedCodePipeline = new Pipeline(this, "reCiterPubmedCodePipeline", PipelineProps.builder()
            .pipelineName("ReCiter-Pubmed-Retrieval-Tool")
            .stages(Arrays.asList(
                StageProps.builder()
                    .stageName("Source")
                    .actions(Arrays.asList(sourceAction))
                    .build(),
                StageProps.builder()
                    .stageName("Build")
                    .actions(Arrays.asList(buildAction))
                    .build(),
                StageProps.builder()
                    .stageName("Approve")
                    .actions(Arrays.asList(manualApprovalAction))
                    .build(),
                StageProps.builder()
                    .stageName("Deploy-to-ECS")
                    .actions(Arrays.asList(deployAction))
                    .build()
            ))
            .build());

        //Pipeline for ReCiter Scopus
        if(System.getenv("INCLUDE_SCOPUS") != null && System.getenv("INCLUDE_SCOPUS").equals("true")) {
            final Pipeline reCiterScopusCodePipeline = new Pipeline(this, "reCiterScopusCodePipeline", PipelineProps.builder()
                .pipelineName("ReCiter-Scopus-Retrieval-Tool")
                .stages(Arrays.asList(
                    StageProps.builder()
                        .stageName("Source")
                        .actions(Arrays.asList(new GitHubSourceAction(GitHubSourceActionProps.builder()
                            .actionName("Github_Source")
                            .owner(System.getenv("GITHUB_USER"))
                            .repo("ReCiter-Scopus-Retrieval-Tool")
                            .branch("master")
                            .oauthToken(SecretValue.plainText(System.getenv("GITHUB_PERSONAL_ACCESS_TOKEN")))
                            .output(reCiterScopusSourceOutput)
                            .build())))
                        .build(),
                    StageProps.builder()
                        .stageName("Build")
                        .actions(Arrays.asList(new CodeBuildAction(CodeBuildActionProps.builder()
                            .actionName("CodeBuild")
                            .project(reCiterScopusCodebuildProject)
                            .input(reCiterScopusSourceOutput)
                            .outputs(Arrays.asList(reCiterScopusbuildOutput))
                            .build())))
                        .build(),
                    StageProps.builder()
                        .stageName("Approve")
                        .actions(Arrays.asList(new ManualApprovalAction(ManualApprovalActionProps.builder()
                            .actionName("Approve")
                            .notificationTopic(reCiterTopic)
                            .build())))
                        .build(),
                    StageProps.builder()
                        .stageName("Deploy-to-ECS")
                        .actions(Arrays.asList(new EcsDeployAction(EcsDeployActionProps.builder()
                            .actionName("DeployAction")
                            .service(reCiterScopusService)
                            .imageFile(new ArtifactPath(reCiterScopusbuildOutput, "imagedefinitions.json"))
                            .build())))
                        .build()
                ))
                .build());
        }

        //Pipeline for ReCiter

        final Pipeline reCiterCodePipeline = new Pipeline(this, "reCiterCodePipeline", PipelineProps.builder()
            .pipelineName("ReCiter")
            .stages(Arrays.asList(
                StageProps.builder()
                    .stageName("Source")
                    .actions(Arrays.asList(new GitHubSourceAction(GitHubSourceActionProps.builder()
                        .actionName("Github_Source")
                        .owner(System.getenv("GITHUB_USER"))
                        .repo("ReCiter")
                        .branch("master")
                        .oauthToken(SecretValue.plainText(System.getenv("GITHUB_PERSONAL_ACCESS_TOKEN")))
                        .output(reCiterSourceOutput)
                        .build())))
                    .build(),
                StageProps.builder()
                    .stageName("Build")
                    .actions(Arrays.asList(new CodeBuildAction(CodeBuildActionProps.builder()
                        .actionName("CodeBuild")
                        .project(reCiterCodebuildProject)
                        .input(reCiterSourceOutput)
                        .outputs(Arrays.asList(reCiterbuildOutput))
                        .build())))
                    .build(),
                StageProps.builder()
                    .stageName("Approve")
                    .actions(Arrays.asList(new ManualApprovalAction(ManualApprovalActionProps.builder()
                        .actionName("Approve")
                        .notificationTopic(reCiterTopic)
                        .build())))
                    .build(),
                StageProps.builder()
                    .stageName("Deploy-to-ECS")
                    .actions(Arrays.asList(new EcsDeployAction(EcsDeployActionProps.builder()
                        .actionName("DeployAction")
                        .service(reCiterService)
                        .imageFile(new ArtifactPath(reCiterbuildOutput, "imagedefinitions.json"))
                        .build())))
                    .build()
            ))
            .build());

        //Pipeline for ReCiter Pub Manager

        final Pipeline reCiterPubManagerCodePipeline = new Pipeline(this, "reCiterPubManagerCodePipeline", PipelineProps.builder()
            .pipelineName("ReCiter-Publication-Manager")
            .stages(Arrays.asList(
                StageProps.builder()
                    .stageName("Source")
                    .actions(Arrays.asList(new GitHubSourceAction(GitHubSourceActionProps.builder()
                        .actionName("Github_Source")
                        .owner(System.getenv("GITHUB_USER"))
                        .repo("ReCiter-Publication-Manager")
                        .branch("master")
                        .oauthToken(SecretValue.plainText(System.getenv("GITHUB_PERSONAL_ACCESS_TOKEN")))
                        .output(reCiterPubManagerSourceOutput)
                        .build())))
                    .build(),
                StageProps.builder()
                    .stageName("Build")
                    .actions(Arrays.asList(new CodeBuildAction(CodeBuildActionProps.builder()
                        .actionName("CodeBuild")
                        .project(reCiterPubManagerCodebuildProject)
                        .input(reCiterPubManagerSourceOutput)
                        .outputs(Arrays.asList(reCiterPubManagerbuildOutput))
                        .build())))
                    .build(),
                StageProps.builder()
                    .stageName("Approve")
                    .actions(Arrays.asList(new ManualApprovalAction(ManualApprovalActionProps.builder()
                        .actionName("Approve")
                        .notificationTopic(reCiterTopic)
                        .build())))
                    .build(),
                StageProps.builder()
                    .stageName("Deploy-to-ECS")
                    .actions(Arrays.asList(new EcsDeployAction(EcsDeployActionProps.builder()
                        .actionName("DeployAction")
                        .service(reCiterPubManagerService)
                        .imageFile(new ArtifactPath(reCiterPubManagerbuildOutput, "imagedefinitions.json"))
                        .build())))
                    .build()
            ))
            .build());

            //Pipeline for ReCiter Pub Manager

        final Pipeline reCiterMachineLearningAnalysisCodePipeline = new Pipeline(this, "reCiterMachineLearningAnalysisCodePipeline", PipelineProps.builder()
            .pipelineName("ReCiter-Machine-Learning-Analysis")
            .stages(Arrays.asList(
                StageProps.builder()
                    .stageName("Source")
                    .actions(Arrays.asList(new GitHubSourceAction(GitHubSourceActionProps.builder()
                        .actionName("Github_Source")
                        .owner(System.getenv("GITHUB_USER"))
                        .repo("ReCiter-MachineLearning-Analysis")
                        .branch("master")
                        .oauthToken(SecretValue.plainText(System.getenv("GITHUB_PERSONAL_ACCESS_TOKEN")))
                        .output(reCiterMachineLearningAnalysisSourceOutput)
                        .build())))
                    .build(),
                StageProps.builder()
                    .stageName("Approve")
                    .actions(Arrays.asList(new ManualApprovalAction(ManualApprovalActionProps.builder()
                        .actionName("Approve")
                        .notificationTopic(reCiterTopic)
                        .build())))
                    .build(),
                StageProps.builder()
                    .stageName("Build")
                    .actions(Arrays.asList(new CodeBuildAction(CodeBuildActionProps.builder()
                        .actionName("CodeBuild")
                        .project(reCiterMachineLearningAnalysisProject)
                        .input(reCiterMachineLearningAnalysisSourceOutput)
                        .outputs(Arrays.asList(reCiterMachineLearningAnalysisbuildOutput))
                        .build())))
                    .build()
            ))
            .build());
        
    }

    public Project getPubmedCodeBuildProject() {
        return this.reCiterPubmedCodebuildProject;
    }
    public Project getScopusCodeBuildProject() {
        return this.reCiterScopusCodebuildProject;
    }
    public Project getReCiterCodeBuildProject() {
        return this.reCiterCodebuildProject;
    }
    public Project getReCiterPubManagerCodeBuildProject() {
        return this.reCiterPubManagerCodebuildProject;
    }
}
