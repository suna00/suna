# CJ 개발서버 새팅

### 0. 기본정보

#### 환경
- Windows 컴퓨터에서 mama VPN 접속
- VDI 사이트 접속(IE 에서만 가능) 
  - URL : http://cjvdi.cj.net
  - 계정 : s-neobud01 / dkdldhs!QAZ
  - VDI 프로그램 자동실행 안되면 Citrix Receiver 켜졌는지 확인
- 가상머신에서 원격접속 통제 시스템 접속
  - URL : https://cjgate.cj.net
  - 계정 : yinkim / dkdldhs!QAZ1

#### 구성
- global-dev01 (CentOS 6.8) : CMS
- global-dev02 (CentOS 6.8) : MySQL
- global-dev03 (Win R2 2008) : MS-SQL (EnM 데이터 마이그레이션용)

#### 기타 정보
- mysql 관리자 : `root` / `1234`
- mysql 파일관리 : MyISAM (배포 버전 5.1. 5.7 이상부터 InnoDB)


### 1. CMS 구성

#### 계정정보
`enmadminuser`/`!@12우리나라`

#### 디렉토리
```
~/ion/cms
         /core
         /admin-builder
```

#### 작업절차

##### A. 패키지 관리자 최신화
```
$ > yum update
$ > yum install vim
```
`yum install epel-release` 가 방화벽 문제로 실행되지 않는다.(`wget` 으로 `rpm` 리파지토리 정보를 불러오지 못함) 

##### B. `jdk` 설치

```
$ > yum install java-1.8.0-openjdk-devel.x86_64
$ > echo "JAVA_HOME=/usr/lib/jvm/jre-1.8.0-openjdk.x86_64" >> ~/.bash_profile
$ > source ~/.bash_profile
```


### 2. MySQL 구성

#### A. 설치
```
$ > yum update
$ > yum install mysql-server
```

#### B. `my.cnf` 수정

```
[mysqld]
... 전략 ...
character-set-server=utf8
collation-server=utf8_unicode_ci
slow_query_log = 1


[mysqld_safe]
... 수정 불필요 ...

[client]
default-character-set=utf8

[mysqldump]
default-character-set=utf8

[mysql]
default-character-set=utf8
```
디폴트 문자셋을 `utf8mb4` 로 할 경우, 메모리 사용이 비효율적이기 때문에 이모티콘 등이 필요 테이블/칼럼에 대해서 개별적으로 문자셋을 관리 바람.


#### C. 서비스 제어

```
$ > service mysqld [start / stop / restart ]
```


### 3. API 구성

`code deploy`, `code commit` 으로 구성
- `code commit` : gitlab 이나 github 같은 원격 소스 저장소
- `code deploy` : jenkins 같은 빌드배포 도구


##### A. 패키지 관리자 최신화
##### B. `jdk` 설치
##### C. `gradle` 설치
