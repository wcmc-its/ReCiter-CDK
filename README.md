# Welcome to ReCiter-CDK Java project!

This project builds all cloud infrastructure in AWS using CDK(Cloud Development Kit). The project creates necessary roles, permissions, ECR repository, VPC, ECS services, RDS etc.

[ReCiter](https://github.com/wcmc-its/reciter/) is a highly accurate system for guessing which publications in PubMed a given person has authored. ReCiter includes a Java application, a DynamoDB-hosted database, and a set of RESTful microservices which collectively allow institutions to maintain accurate and up-to-date author publication lists for thousands of people. This software is optimized for disambiguating authorship in PubMed and, optionally, Scopus. 

ReCiter can be installed on a locally controlled server or using services provided by Amazon Web Services (AWS). For those looking to install ReCiter on AWS, this cloud repository provides a easy way to install in the cloud.

The `cdk.json` file tells the CDK Toolkit how to execute your app.

It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.

## Installation instructions

### Stack Environment Variables
The environment variables needed for stack to run.

|Key | Value |
|---|---|
| ADMIN_API_KEY | Admin Api key for ReCiter |
| CONSUMER_API_KEY | Consumer Api key for ReCiter |
| PUBMED_API_KEY   | Use a pubmed api key unless you want to be throttled |
| INCLUDE_SCOPUS | Specify whether to include Scopus, do it if you have scopus subscription - Accepts Boolean (true/false) |
| ALARM_EMAIL | The email address where all alerts be sent(valid email address) |
| GITHUB_USER | Github username of your account |
| GITHUB_PERSONAL_ACCESS_TOKEN | Personal access token of github account |
| SCOPUS_API_KEY | Input this if you have selected true for INCLUDE_SCOPUS |
| SCOPUS_INST_TOKEN | Input this if you have selected true for INCLUDE_SCOPUS |

Use `export <environment variable name>=<value>`

### Required software/packages
- Install the AWS CLI (command-line interface) on your local machine. 
   - Verify that you have Python installed, preferably Python 3.4 or greater. 
      - To check on your version of Python, enter the following in Terminal: `python --version`
      - If Python is not the proper version, enter: `brew install python`
   - Use PIP to install the AWS CLI: `pip3 install awscli --upgrade --user` 
   - Check version using `aws --version`
   - Setup AWS profile
     - In Terminal, enter `aws configure --profile reciter`
     ![add user](/files/image4.png)
     - Input: the access key and secret key you got from creating the AWS user; also, input the region you want to run ReCiter in. 
     - Your profile should now be set up, but let's run a test to see if its setup we will use the cli to get our AWS account number in terminal - `aws sts get-caller-identity --output text --query 'Account' --profile reciter` and you should get account number

- Install Brew(if not installed - macOS Users)
    - Go to Terminal - `/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"`
    - Run `brew update` to make sure brew is updated

- Install Nodejs and AWS CDK
    - brew install node
    - Install cdk `npm install -g aws-cdk`
    - Check installation of cdk and version `cdk --version`

- Create an AWS user and account through AWS console
   - Create an AWS user [here](https://console.aws.amazon.com/console/home).
   - Navigate to the [IAM](https://console.aws.amazon.com/iam/home) (Identity and Access Management) managed service.
   - Go to `Users` and click on `Add User`
   ![add user](/files/image6.png)   
   - User name could be anything, but let's choose `svc-reciter.`
   ![add user](/files/image8.png)
   - For `Access Type`, select as `Programmatic Access.`
   - Click on `Next Permissions.`
   - Click on `Attach existing policies directly.`
   - Use the filter to find and select the policy, `AdministratorAccess.`
   ![add user](/files/image5.png)
   - Click on `Next Review`
   - Create user.
   - Click on `Download credentials.` (You will only able to view the credential once. Store in a secure location.)

- Configure your GitHub account to get Personal access tokens
   - If you haven't done so already, create a [Github Account](https://github.com/).
   - Visit the [settings](https://github.com/settings/profile), associated with your personal Github account. 
   - Click on `Developer Settings`
   - Go to [Personal Access Tokens](https://github.com/settings/tokens).
   - Click on `Generate new token`.
   ![add user](/files/image7.png)
   - In the `Note` field, enter `reciter` (or whatever alternative you wish).
   - Check `public_repo` and `admin:repo_hook` and then click on `Generate token` button below.
   ![add user](/files/PersonalAccessToken.png)
   - Note this token in a secure place.

- Fork the ReCiter repository to your personal GitHub account.
   - Go to the [ReCiter repository](https://github.com/wcmc-its/ReCiter).
   - Click on the `Fork` button.	
   - Go to the application,properties file as it is forked on your personal account as located here: 
   `https://github.com/<your-github-username>/ReCiter/blob/master/src/main/resources/application.properties`
   - Edit the file and find `aws.s3.use.dynamic.bucketName` and set that flag = `true`:
   `aws.s3.use.dynamic.bucketName=true`
   - Under commit message, enter `Dynamic bucket generation`. 
   - Click `Commit`
   - If you dont want to use scopus(or dont have a subscription) then ddit the file and find `use.scopus.articles` and set that to false:
   `use.scopus.articles=false`
   - Under commit message, enter `set scopus flag`. 
   - Click `Commit`

- Fork the ReCiter-Pubmed-Retrieval tool repository to you personal GitHub account.
   - Go to the [ReCiter PubMed Retrieval Tool](https://github.com/wcmc-its/ReCiter-PubMed-Retrieval-Tool).
- Fork the ReCiter-Scopus-Retrieval-Tool repository to you personal GitHub account. Do this if you have set `INCLUDE_SCOPUS` flag as true and also if you have valid scopus subscription.
   - Go to the [Scopus repository](https://github.com/wcmc-its/ReCiter-Scopus-Retrieval-Tool)
- Fork the ReCiter-Publication-Manager repository to you personal GitHub account.
   - Go to the [ReCiter publication manager repository](https://github.com/wcmc-its/ReCiter-Publication-Manager)
   - Click on the `Fork` button. 

- We are all set to do the installation
    - Go to your terminal where you have cloned this repository. If not cloned then use `git clone https://github.com/wcmc-its/ReCiter-CDK.git`
    - Run `cdk synth` and this will synthesize all the cloudformation template for you in `cdk.out` folder. If also if you have not environment variables properly it will prompt you.
    - Run `cdk deploy --profile reciter` reciter is thr profile name you gave when you set up the aws-cli with `aws configure --profile reciter`
    - This should take some time and you can login to AWS and go to Cloudformation to see the progress. After it is installed you can go to the ECS Stack Output section to see the url for ReCiter and its components.
    - Remember all the resources here will be publicly accessible over the internet. Although we have Web appllication firewall providing necessary security against malicious bots and others. It is advisable to use it using SSL and valid certificate.
### Enjoy ReCiter - For any questions email Sarbajit Dutta(szd2013@med.cornell.edu)

## Useful commands

 * `mvn package`     compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation

Enjoy!
