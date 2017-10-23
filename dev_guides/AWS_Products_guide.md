# AWS 사용 가이드


## 0. 제품군

- IAM : 사용자 관리 시스템
- EC2 : 클라우드 이미지 인스턴스
- S3 : 클라우드 스토리지
- RDS : 데이터 베이스
- CodeCommit : 개발 소스 스토리지
- CodeBuild : 소스 빌드 시스템
- CodeDeploy : 소스 배포 시스템
- CodePipeline : 자동 빌드/배포 시스템
- CloudWatch : 인스턴스 모니터링 시스템 

각 제품군에 접근하기 위해서는 아마존 콘솔 계정이 필요함. `CodeCommit` 같은 서비스는 콘솔 계정 이외에 서비스 사용자 계정이 별도로 존재함.

계정에 제공되는 `access key`, `secret access key` 는  수정이 불가능하고 재발급만 가능함. 

## 1. 용어 정리
- 콘솔 : 터미널이 아니라 Web 에서 제공하는 아마존 관리자 UI 를 지칭함
- AMI : Amazon Machine Image. 가상 이미지
- 버킷 : S3 저장소. 예를 들면 S3  제품 내의 ion 경로.

## 2. S3 연동 - java

##### A. 초기화

S3 파일 연동을 하기 위해서는 `S3Client` 인스턴스를 생성해야 함. `Gradle` 프로젝트의 경우 디펜던시에 `compile ("com.amazonaws:aws-java-sdk-s3:${VERSION}")` 를 추가해야 한다.

인스턴스 생성 예제. `build` 메소드로 생성된 `S3Client` 객체는 immutable 이기 때문에 설정을 변경하거나 재생성할 수 없다.
```
private void initializeS3Client () throws Exception {
    awsCredentials = new BasicAWSCredentials(accessKey, secretAccessKey);
    s3Client = AmazonS3ClientBuilder.standard()
            .withRegion("ap-northeast-2") //로컬에서 오류나면 해제
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .withAccelerateModeEnabled(true)
            .build();
    logger.info("bucketLocation with client :: " +       
                s3Client.getBucketLocation(bucketName));
}
```
- region 을 명시하지 않아도 되지만, 관련 오류가 발생할 경우 `withRegion` 으로 명시적으로 지정해서 해결할 수 있음
- 속도가 느린 경우, `withAccelerateModeEnabled(true)` 로 속도를 증가시킬 수 있음(유료 서비스) - 이 경우, bucket url 이 `*.s3.*` 에서 `*.s3-accelerate.*` 로 변경된다

##### B. 업로드 

`S3Client.putObject(String buckName, String bucketKey, File file)` 을 사용해서 파일 업로드를 구현할 수 있음. `bucketKey` 라고 부르는 부분은 업로드할 파일의 풀 경로이다(파일이름 포함).

```
private PutObjectResult uploadFile (String dirPath, File file) throws Exception {
    String fileName = file.getName();
    String fullBucketPath = null;
    if(!dirPath.startsWith(defaultBucketKey)) {
        dirPath = defaultBucketKey + (dirPath.startsWith("/") ? dirPath : ("/" + dirPath));
    }

    if(!dirPath.endsWith(file.getName())) {
        fullBucketPath = dirPath + (dirPath.endsWith("/") ? dirPath : (dirPath + "/")) + fileName;
    } else {
        fullBucketPath = dirPath;
    }

    logger.info("Send File [ " + fileName + " ] to S3(included FileName) [ " + fullBucketPath + " ]" );
    return s3Client.putObject(new PutObjectRequest(bucketName, fullBucketPath, file));
}
```

##### C. 외부접근
아마존 콘솔 S3 메뉴에서 웹 파일 탐색기로 업로드한 파일과 접근 url 이 조회 가능함. 단 해당 버킷이 `public` 으로 설정되어 있지 않으면 콘솔 외부의 웹에서 이미지 조회가 불가능함.


## 3. RDS 설정 정보

`RDS` 는 EndPoint 가 URL 형식으로 배포되며 `ssh` 를 통해 접근할 수 없음. 

`my.cnf` 와 같은 설정은 아마존 콘솔에서 가능함. 기본 charset 이나 시간대 등을 변경한 이후에는 RDS 인스턴스를 재시작해야 반영됨 

## 4. CodeCommit

아마존 콘솔에서 사용하는 `git`. git  에서 사용하는 모든 기능을 사용가능 함. IdeaProjects 에서 공식 플러그인을 지원하지 않음.

##### CodeCommit 인스턴스 생성 
- 아마존 콘솔에서 `CodeCommit` 메뉴 진입
- `레파지토리 생성`
- `IAM` 사용자 자격증명 생성
    - 이 자격증명이 `git` 명령에서 필요하기 때문에 잘 보관할 것(잊어버리면 찾기나 수정이 불가하고 사용자 자체를 재생성해야 하기 때문에 번거로움)

##### IntelliJ 에서 CodeCommit 연동
준비물 :
- intellij 의 terminal 탭
- local 에 git 이 설치되어 있어야 함

```
# Intellij 터미널에서 

# 자신이 프로젝트 최상위 경로인지 확인한다
$ > pwd

# 현재 연결된 remote branch 확인
$ > git remote -v

# CodeCommit 브랜치 추가 * "yourCustomCodeCommit" 대신 사용할 이름을 적으면 됨
$ > git remote add yourCustomCodeCommit https://git-codecommit.{region}/v1/repo/{repo}

# 현재 변경점 커밋 ** 아래 내용 추가 참조
$ > git commit -m "PPAP"

# 코드 커밋에 반영
$ > git push yourCustomCodeCommit
```
** 이 경우, 원래 사용하던 원격 브랜치와 코드커밋 브랜치를 같이 사용하기 때문에 UI 에서 commit 된 내용이 `git push yourCustomCodeCommit` 에 같이 올라갈 수 있으니 주의해야 함.


## 5. CodeDeploy

코드 배포 시스템. `Jenkins` 처럼 빌드하는 기능은 없음. 오토스케일 그룹이나 복수의 인스턴스에 `이미 빌드된 소스`를 배포하는 역할

##### 선행 작업
CodeDeploy 를 사용하기 위해서는 아래와 같은 작업들이 선행되어야 함
1. CodeDeploy 의 대상이 되는 EC2 인스턴스에 `codedeploy-agent` 가 설치되어야 함 (agent 의 선행으로는 ruby 2.x 이상이 설치되어 있어야 함. 수동으로 처리가 필요한 부분 )
2. CodeDelpoy 성공 후, 해당 서비스를 `ELB` (도메인) 서비스 그룹으로 포함시키기 위해 web 으로 호출할 수 있는 URL 이 제공되어야 함(health check  페이지)
3. health check 페이지를 호출할 수 있도록 장비 재기동시 서비스 애플리케이션을 자동으로 구동하는 서비스가 구현되어 떠야 함(가령 Linux 계열의 경우, 톰캣을 자동 기동하는 서비스를 만들어 `/etc/init.d/` 에 추가하고 `chkconfig` 서비스에 추가해놔야 함)
4. CodeDeploy 에서 선행된 작업을 참조할 수 있도록 `appspec.yml` 이 작성되어야 함 (이름 변경 불가. `yaml` 확장 사용불가)

##### CodeDeploy 생성

- 아마존 콘솔에서 `CodeDeploy` 메뉴 진입
- `애플리케이션 생성`
- 애플리케이션 이름과 배포 그룹 이름은 사용자 지정 필드이므로 마음대로 작성
- 배포 유형 : 현재 위치 배포, 블루/그린 배포 **
- 환경 구성 : Auto Scaling 그룹, EC2 인스턴스, 온프레미스 인스턴스가 있음. 선택 후 `일치하는 인스턴스` 항목에 선택 그룹/조건에 맞는 인스턴스 목록이 제공되니 확인할 것
    - Auto Scaling 그룹 :  기존에 스케일 그룹이 등록되어 있어야 함
    - EC2 인스턴스 : 인스턴스 단위로 배포 복수 선택 가능
    - 온프레미스 : `안해봄`
- 배포 구성 : 한번에 한개 혹은 여러개 배포할지 선택 
- 서비스 역할 : 기존에 IAM 에서 설정한 권한을 선택할 수 있음

##### CodeDeploy 상세

- 배포 그룹 : 생성 단계에서 적용한 내용을 조회/수정할 수 있고, `CloudWatch` 로 감시하다 특정 조건에 도달했을 때 알람을 제공할 수 있음
- 개정 : 배포된 내역을 표기함.

##### appspec.yml 만들기 

```
version: 0.0
os: linux
files:
  - source: / #<- 배포할 repository path
    destination: /home/ion/api/core #<- 배포될 서버의 path
hooks:
  BeforeInstall:
    - location: ice-clean.sh
      timeout: 30
      runas: ion

  ApplicationStart:
    - location: ice-start.sh #<- BeforeInstall 단계에 실행할 script
      timeout: 30 #<- script 실행 timeout
      runas: ion #<- 위 스크립트 실행 user

  ApplicationStop:
    - location: ice-stop.sh
      timeout: 30
      runas: ion

permissions:
  - object: /home/ion/api
    owner: ion
    group: ion
    mode: 755
```
- `version` : CodeDeploy 의 버전. 수정 불가
- `os` : 대상 운영체제
- `files.source` : 배포툴의 경로. 가령 `Jenkins` 를 사용한다면 `Jenkins` 해당 워크스페이스의 하위 경로.
- `files.destination` : 대상 `EC2` 의 output 경로
- `hooks` : CodeDeploy 의 이벤트 리스너. 아마존 콘솔의 CodeDeploy 상세에서 보면 어느 순서로 실행되는지 볼 수 있음.
- `hooks.{event}.location` : 배포 후 실행될 스크립트의 `대상 EC2 내의 절대경로`. 파라미터 전달 불가 (`ice.sh start`-X) 
- `hooks.{event}.timeout` : 너무 짧게 주면 해당프로세스를 완료하지 않았는데 다음 이벤트를 실행해서 실패하게 된다
- `hooks.{event}.runas` : 해당 이벤트를 처리할 사용자. default 는 `root`
- `permissions.object` : 해당 경로 이하의 모든 디렉토리의 권한을 적용함
- `permissions.owner` : `permissions.object` 에서 지정한 자원의 사용자를 설정함
- `permissions.group` : `permissions.object` 에서 지정한 자원의 그룹을 설정함
- `permissions.mode` : `permissions.object` 에서 지정한 자원의 권한을 설정함 

## 6. Jenkins 연동

##### 준비 플러그인
- AWS CodeCommit Trigger Plugin
- AWS CodePipline Plugin
- SSH Slaves Plugin
- (선택) Gitlab Authentication Plugin
- (선택) GitHub Authentication Plugin
- Gradle Plugin
- Publish Over SSH
- Publish Over FTP

##### 프로젝트 생성
1. Free-Style 프로젝트 생성
2. 필요한 경우 concurrent 빌드 실행 체크
3. `소스코드 관리` : AWS CodePipline 선택
    - `AWS Access Key / AWS Secret Access Key` : `CodeCommit` 사용자의 엑세스 키. 콘솔 사용자와 혼동하지 말 것
   - `Provider` : 이 항목에 작성한 이름이 추후 `CodePipeline 빌드 공급자` 항목에 선택항으로 제공됨 
4. `빌드 유발` : Poll SCM 체크
   - `CodeCommit` 의 변경점을 얼마마다 감지할 것인지. 공식 예제에서는 `* * * * *` 로 되어 있음 
5. `Build` : 프로젝트에 맞는 그레들 빌드 설정
   - 예시 `clean build -x test --refresh-dependencies`
6. `빌드 후 조치` : `AWS CodePipeline Publisher`
   - `Location` : 해당 로케이션에 있는 내용을 코드파이프 라인 다음 단계로 전달함
   - `Artifact Name` : `Location` 항목에 있는 내용을 압축하여 작성한 Name 으로 전달됨

##### 주의 사항
`Gradle` 프로젝트의 경우, 빌드된 `war` 를 배포할 때 주의사항이 있음

1. `AWS CodePipeline Publisher` 의 `Location` 에 war 를 명시한 경우    
    war 안에서 `appspec.yml`을 참조하기 위해서는 appspec 이 프로젝트의 `src/main/webapp` 안에 있어야 함. 또한 배포 대상 EC2 에서 보면 war의 형태가 아니라 webapp 안의 내용만 복사된 것을 확인할 수 있음. 
    
    `appspec` 의 `source` 항목의 `/` 가 `/webapp` 으로 맵핑된다는 결론
    
2. 해결안
    `build.gradle` 에서 war 의 ouput 경로를 변경하고, 해당 경로에 `appspec` 을 같이 둠. 그리고 Jenkins 의 Location 에서는 해당 경로를 참조함.
    
문제점을 해결하기 위해 작성한 shell script. 스크립트를 Jenkins 와 같은 서버 인스턴스에 놓고, UI 에서 배포 전에 스크립트를 실행 시켰음

아래 스크립트는 몇가지 전제조건이 있음 

- 스프링 부트 프로젝트에서 각 프로파일 별로 `codedeploy` 라는 경로가 존재하고, 최상위에도 `codedeploy` 라는 디렉토리가 존재한다는 가정이 있음. 
- 빌드를 하면 output 을 최상위의 `codedeploy` 로 전달함

`build.gralde` 의 `war` 태스크 설정
```
war {
    baseName = 'ice2-core'
    manifest {
        attributes('Main-Class': 'net.ion.ice.Ice2Application')
    }
    destinationDir = file("$projectDir/codedeploy")
}
```

Jenkins 에서 `Publish Over SSH` 로 구동될 쉘.
```
#! /bin/bash

echo -e "Prepare for Amazon Codedeploy"

export JENKINS_WORKDIR=/var/lib/jenkins/workspace
export JENKINS_PROJ=AWS-CORE
export SPRING_PROFILE=dev

function guide () {
	echo -e "Usage codedeploy-prepare [ jenkins.projectname ]:[ spring.profile.active ]"
}

function splitParams () {
	IFS=":" read -ra PARAMS <<< $1
	JENKINS_PROJ=${PARAMS[0]}
	SPRING_PROFILE=${PARAMS[1]}
}

function mvCodeDeployDir () {
	cp $JENKINS_WORKDIR/$JENKINS_PROJ/src/main/resources/codedeploy/$SPRING_PROFILE/* $JENKINS_WORKDIR/$JENKINS_PROJ/codedeploy	
}

# 주1) 만약 파라미터가 전달되지 않았다면 콘솔에 출력하고 에러로 종료한다
if [ $# -lt 1 ] 
then
	echo "Wrong parameter counts"
	guide
	exit 1
fi

# 주2) 전달받은 파라미터를 [젠킨스 프로젝트명]:[스프링부트 사용자 프로파일] 로 끊어낸다
splitParams $1

# 주3) 각 스프링부트 프로파일명으로 되어 있는 폴더를 찾아 최상위 codedeploy 라는 폴더로 쉘파일과 빌드 완료 파일을 이동한다
mvCodeDeployDir

# 주4) 상기 프로세스간 오류가 발생했다면 오류 종료를 하고 아니라면 정상 종료한다
if [ $? -ne 0 ]
then
	echo "Failed to prepare for Amazon Codedeploy"
	exit 1
fi

echo -e "All Files are ready to deploy via Codedeploy"

exit 0
```


## 7. CodePipeline

`CodeCommit` 에 등록된 소스를 빌드 툴로 빌드하여 `CodeDeploy` 로 배포할 수 있도록 워크플로우를 관리하는 제품

##### CodePipeline 생성
- 아마존 콘솔에서 `CodePipeline` 메뉴 진입
- `파이프라인 생성`
- 1단계 이름 : 항목은 사용자 지정 항목임
- 2단계 소스 : 배포시 원천 소스의 제공자를 선택함 (`CodeCommit` 의 경우, repo 와 브랜치를 선택할 수 있음)
- 3단계 빌드 : 빌드 공급자를 선택한다. 선행으로 `Jenkins` 같은 빌드 툴이 생성되어 있어야 함(콘솔 외부에서 처리됨)
    - `Jenkins` 를 선택할 경우, `ProjectName` 을 제공하는데 반드시, Jenkins 에서 설정한 이름과 동일해야 함
- 4단계 배포 : 배포 공급자를 선택함
    - `CodeDeploy` 를 선택할 경우, `애플리케이션 이름`/`배포 그룹` 을 받는데, 반드시 CodeDeploy 에 설정된 내용과 일치하여야 함
- 5단계 서비스 역할 : IAM 에서 기 생성된 역할 중 선택
- 생성

##### 배포시 승인 절차 추가하기
기본 설정만한 경우, 수시로 `CodeCommit` 의 내용이 빌드됨. 관리자가 반영될 소스의 내역을 검토하기 위해서 `승인` 절차를 추가함
- CodePipeline 상세 진입
- `편집`
- `Source`, `Build` 사이에 `+ 단계` 클릭
- 새로 오픈된 박스에서 작업명 입력하고 `+ 작업` 클릭
- 작업범주 셀렉트박스에서 `승인` 선택
- 승인 유형 `수동 승인`
- SNS 주제 ARN : IAM 에서 SNS ARN 이 생성되어 있어야 하고**, 기존에 선택된 ARN 중에서 선택 - SNS 설정에서 승인 수신자를 설정 가능
- 내용 입력하고 `작업 추가`

## 8. CloudWatch

`AWS` 에서 사용되는 인스턴스를 모니터링할 수 있는 tool. `Kibana` 같은 기능이라고 볼 수 있음. 노출되는 정보는 아래와 같음

- CPU 사용률
- 메모리 사용률
- ELB 인스턴스 수
- 지정한 그룹의 Active 서버 카운트
- 서버의 디스크 사용률
- Netwotk In/Out
- RDS CPU
- RDS 메모리
- RDS 커넥션 수
- RDS 사용가능 스토리지


## 99. 변경 내역
- 20171022 : 초안 작성