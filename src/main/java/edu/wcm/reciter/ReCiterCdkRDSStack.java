package edu.wcm.reciter;

import org.json.JSONObject;

import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.NestedStack;
import software.amazon.awscdk.core.NestedStackProps;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Tags;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.DatabaseInstanceProps;
import software.amazon.awscdk.services.rds.LicenseModel;
import software.amazon.awscdk.services.rds.MariaDbEngineVersion;
import software.amazon.awscdk.services.rds.MariaDbInstanceEngineProps;
import software.amazon.awscdk.services.rds.StorageType;
import software.amazon.awscdk.services.rds.SubnetGroup;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;

public class ReCiterCdkRDSStack extends NestedStack {
    
    public ReCiterCdkRDSStack(final Construct parent, final String id) {
        this(parent, id, null, null, null);
    }

    public ReCiterCdkRDSStack(final Construct parent, final String id, final NestedStackProps props, IVpc vpc, SubnetGroup privateDbSubnetGroup) {
        super(parent, id, props);

        final DatabaseInstance reciterDb = new DatabaseInstance(this, "reciterDb", DatabaseInstanceProps.builder()
            .engine(DatabaseInstanceEngine.mariaDb(MariaDbInstanceEngineProps.builder()
                .version(MariaDbEngineVersion.VER_10_5_9)
                .build()))
            .vpc(vpc)
            .allocatedStorage(50)
            .publiclyAccessible(true)
            .allowMajorVersionUpgrade(true)
            .autoMinorVersionUpgrade(true)
            .backupRetention(Duration.days(7))
            .copyTagsToSnapshot(true)
            .databaseName("reciter")
            .deletionProtection(true)
            .enablePerformanceInsights(false) //Not supported
            .instanceIdentifier("reciter-report-db")
            .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.SMALL))
            .multiAz(false)
            .licenseModel(LicenseModel.GENERAL_PUBLIC_LICENSE)
            .port(3306)
            .removalPolicy(RemovalPolicy.DESTROY)
            .storageType(StorageType.GP2)
            .subnetGroup(privateDbSubnetGroup)
            .preferredBackupWindow("01:00-02:00")
            .preferredMaintenanceWindow("Sat:03:00-Sat:05:00")
            .credentials(Credentials.fromSecret(Secret.Builder.create(this, "reciter-report-secret")
            .description("This secret houses database credentials for reciter-report-db")
            .generateSecretString(SecretStringGenerator.builder()
                .generateStringKey("password")
                .secretStringTemplate(new JSONObject()
                    .put("username", "admin") 
                    .put("password", "")
                    .toString())
                .excludeNumbers(true)
                .excludeCharacters("/\"@")
                .excludePunctuation(true)
                .passwordLength(16)
                .build())
            .removalPolicy(RemovalPolicy.DESTROY)
            .secretName("reciter-report-secret")
            .build()))
            .build());
            
        //Tagging for all Resources
        Tags.of(this).add("application", "reciter");
        Tags.of(this).add("stack-id", ReCiterCDKECSStack.of(this).getStackId());
        Tags.of(this).add("stack-name", ReCiterCDKECSStack.of(this).getStackName());
        Tags.of(this).add("stack-region", ReCiterCDKECSStack.of(this).getRegion());

        CfnOutput.Builder.create(this, "reCiterReportDBEndpoint")
            .description("ReCiter Report DB endpoint")
            .exportName("reCiterReportDBEndpoint")
            .value(reciterDb.getDbInstanceEndpointAddress())
            .build();
        
        CfnOutput.Builder.create(this, "reCiterReportDBPassword")
            .description("ReCiter Report DB password")
            .exportName("reCiterReportDBPassword")
            .value(reciterDb.getSecret().secretValueFromJson("password").toString())
            .build();

        CfnOutput.Builder.create(this, "reCiterReportDBUsername")
            .description("ReCiter Report DB username")
            .exportName("reCiterReportDBUsername")
            .value("admin")
            .build();
    }
    
}
