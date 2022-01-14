## Purpose

# ReCiter-CDK Java project

- [Purpose](#purpose)
- [Dependencies](#dependencies)
- [Installation instructions](#installation-instructions)
- [Useful commands](#useful-commands)
- [Troubleshooting](#troubleshooting)
- [Follow up](#follow-up)


## Purpose 

This project builds all cloud infrastructure in AWS using CDK (Cloud Development Kit). It includes all the necessary roles, permissions, ECR repository, VPC, ECS services, RDS etc. for all the major components of ReCiter, ReCiter Publication Manager, ReCiterDB, ReCiter PubMed Retrieval Tool, and optionally ReCiter Scopus Retrieval Tool.

[ReCiter](https://github.com/wcmc-its/reciter/) is a highly accurate system for guessing which publications in PubMed a given person has authored. ReCiter includes a Java application, a DynamoDB-hosted database, and a set of RESTful microservices which collectively allow institutions to maintain accurate and up-to-date author publication lists for thousands of people. This software is optimized for disambiguating authorship in PubMed and, optionally, Scopus. 

ReCiter can be installed on a locally controlled server or using services provided by Amazon Web Services (AWS). For those looking to install ReCiter on AWS, this cloud repository provides a easy way to install in the cloud.

The `cdk.json` file provides instructions for AWS's CDK Toolkit for installing your app and all the necessary dependencies.

This project is a [Maven](https://maven.apache.org/)-based project, so you can open this project with any Maven compatible Java IDE to build and run tests.

Enjoy!


## Dependencies

The following are the technical dependencies. With the exception of Homebrew and Python, installation instructions are provided below.

- [Homebrew](https://brew.sh/) - tool for installing libraries on your Mac
- [Python](https://www.python.org/) - preferably Python 3.4 or greater
- [PIP](https://pip.pypa.io/en/stable/) - package installer for Python
- [AWS CDK](https://aws.amazon.com/cdk/) - Amazon Web Services Cloud Development Kit
- [AWS CLI](https://aws.amazon.com/cli/) - Amazon Web Services command-line interface
- [Nodejs](https://nodejs.org/en/) - a JavaScript library used by Publication Manager

This CDK can work on non-Mac systems, but these instructions were written with Mac users in mind.




## Installation instructions

**1. Install the AWS CLI (command-line interface) on your local machine.** 
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


**2. Install Homebrew (if not installed by default as is the case for many macOS users)**
  - Enter the following in Terminal - `/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"`
  - Run `brew update` to make sure brew is updated


**3. Install Nodejs and AWS CDK**
  - brew install node
  - Install cdk `npm install -g aws-cdk`
  - Check installation of cdk and version `cdk --version`


**4. Create an AWS user and account through AWS console**
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


**5. Configure your GitHub account to get Personal access tokens**
 - If you haven't done so already, create a [Github Account](https://github.com/).
 - Visit the [settings](https://github.com/settings/profile), associated with your personal Github account. 
 - Click on `Developer Settings`
 - Go to [Personal Access Tokens](https://github.com/settings/tokens).
 - Click on `Generate new token`.
   
![add user](/files/image7.png)

 - In the `Note` field, enter `reciter` (or whatever alternative you wish).
 - Check `public_repo` and `admin:repo_hook` and then click on `Generate token` button below.
   ![add user](/files/PersonalAccessToken.png)
 - Record this token in a secure location.


**6. Fork the ReCiter repository to your personal GitHub account.**
 - Go to the [ReCiter repository](https://github.com/wcmc-its/ReCiter).
 - Click on the `Fork` button.	
 - Go to the application,properties file as it is forked on your personal account as located here: 
   `https://github.com/<your-github-username>/ReCiter/blob/master/src/main/resources/application.properties`
 - Edit the file and find `aws.s3.use.dynamic.bucketName` and set that flag = `true`:
   `aws.s3.use.dynamic.bucketName=true`
 - Under commit message, enter `Dynamic bucket generation`. 
 - Click `Commit`
 - If you dont want to use scopus(or dont have a subscription) then edit the file and find `use.scopus.articles` and set that to false:
   `use.scopus.articles=false`
 - Under commit message, enter `set scopus flag`. 
 - Click `Commit`


**7. Fork the ReCiter-Pubmed-Retrieval tool repository to you personal GitHub account.**
 - Go to the [ReCiter PubMed Retrieval Tool](https://github.com/wcmc-its/ReCiter-PubMed-Retrieval-Tool).


**8. Optional: fork the ReCiter-Scopus-Retrieval-Tool repository to you personal GitHub account. You will need to do this if you have set the `INCLUDE_SCOPUS` flag as true. Note that this requires you have a valid Scopus key, which requires a subscription.**
 - Go to the [Scopus repository](https://github.com/wcmc-its/ReCiter-Scopus-Retrieval-Tool)
 - Click on the `Fork` button. 

**9. Fork the ReCiter-Publication-Manager repository to you personal GitHub account.**
 - Go to the [ReCiter publication manager repository](https://github.com/wcmc-its/ReCiter-Publication-Manager)
 - Click on the `Fork` button. 


**10. Fork the ReCiter-Machine-Learning-Analysis repository to you personal GitHub account.**
 - Go to the [ReCiter machine learning analysis repository](https://github.com/wcmc-its/ReCiter-Machine-Learning-Analysis)
 - Click on the `Fork` button. 


**11. Enter the environmental variables for the stack.**

|Key | Value |
|---|---|
| ADMIN_API_KEY | Admin API key for ReCiter as defined by you |
| CONSUMER_API_KEY | Consumer API key for ReCiter as defined by you |
| PUBMED_API_KEY   | Use a PubMed API key unless you want to be throttled; see [here](https://github.com/wcmc-its/ReCiter-PubMed-Retrieval-Tool/blob/master/README.md#obtaining-an-api-key) for instructions |
| INCLUDE_SCOPUS | Specify whether to include Scopus. Do this if you have Scopus subscription - accepts Boolean (true/false) |
| ALARM_EMAIL | The email address where all alerts be sent; should be valid email address |
| GITHUB_USER | Username of your GitHub account |
| GITHUB_PERSONAL_ACCESS_TOKEN | Personal access token of your GitHub account |
| SCOPUS_API_KEY | Input this if you have selected true for INCLUDE_SCOPUS. See [here](https://github.com/wcmc-its/ReCiter-scopus-Retrieval-Tool/blob/master/README.md#obtaining-an-api-key-and-inst_token). |
| SCOPUS_INST_TOKEN | Input this if you have selected true for INCLUDE_SCOPUS. See [here](https://github.com/wcmc-its/ReCiter-scopus-Retrieval-Tool/blob/master/README.md#obtaining-an-api-key-and-inst_token). |

To input an environmental variable, input in the Terminal `export <environment variable name>=<value>`



**12. Perform the installation**
  - Go to your terminal where you have cloned this repository. If not cloned then use `git clone https://github.com/wcmc-its/ReCiter-CDK.git`
  - Run `cdk synth --profile <profile-name>` and this will synthesize all the cloudformation template for you in `cdk.out` folder. If also if you have not environment variables properly it will prompt you.
  - Run `cdk bootstrap --profile <profile-name>` to create a s3 bucket to house the templates where `<profile-name>` is the profile name you gave when you set up the aws-cli with `aws configure --profile reciter`
  - Run `cdk deploy --profile reciter` reciter is the profile name you gave when you set up the aws-cli with `aws configure --profile reciter`
  - This should take some time and you can login to AWS and go to Cloudformation service to see the progress. After it is installed you can go search for ECS and click on Stack and go to Output section to see the url for ReCiter and its components. 

![output user](/files/CloudFormation.png)


 - Remember all the resources here will be publicly accessible over the internet. Although we have a web application firewall providing necessary security against malicious bots and others, it is advisable to use it using SSL and valid certificate.
 - You will receive the email you entered for `ALARM_EMAIL`. Click on that to confirm the subscription. You will also receive emails when pipeline runs to get approval. You have to approve all the pipeline by clicking the links you get in the email.
 - The ReCiter-Publication-Manager will not work untill and unless you manually approve the pipeline. To do so, go to AWS console and then to CodePipeline then click on `ReCiter-Publication-Manager`. The Pipeline should be at the Approve stage. Click on Review and then Accept. You will also receive an email with the email you have entered for `ALARM_EMAIL`.




## Useful commands

 * `mvn package`     compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation


## Troubleshooting


Q: I'm currently having trouble logging into the ReCiter publication manager. I created users through the API and can use the API to verify that they can authenticate, but when I try to use the same username/password at the login form, I always get "bad credentials".

A: See the final step. You must re-run the pipeline for ReCiter-Publication-Manager in CodePipeline service in AWS. Since it uses the same load balancer URL to reference both ReCiter and ReCiter-Pubmed-Retrieval-Tool and we get the load balancer URL after the stack is deployed.
 



## Follow up

For any questions email [Sarbajit Dutta](szd2013@med.cornell.edu)
