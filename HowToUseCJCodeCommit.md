# AWS CodeCommit 적용 방법

#### CodeCommit
AWS 에서 제공하는 git 기반의 소스 리파지토리 


#### AWS Console 접속 정보

- [AWS Console](https://aws.amazon.com/ko/) 로그인 / (`prd_devops@ion` / `Dkdldhs0!@#`)
- `All Services` > `Developer Tools` > `CodeCommit`
- `ICE2-CORE-PRD` : 아이온 커뮤니케이션 운영 ICE2-CORE
- `URL 복제` > `HTTPS` (https://git-codecommit.ap-northeast-2.amazonaws.com/v1/repos/ICE2-CORE-PRD)

#### IntelliJ Integration

- IntelliJ 에서 공식으로 지원하지 않음
- 프로젝트 열고 `Terminal` 탭으로 진입
```
# 현재 remote 브랜치 확인
$ > git remote -v

# URL 복제에서 가져온 https URL 을 remote 에 추가
$ > git remote add codecommit https://git-codecommit.ap-northeast-2.amazonaws.com/v1/repos/ICE2-CORE-PRD

# remote 브랜치 업데이트
$ > git remote update

# codecommit remote 브랜치에 소스 올리기
$ > git push codecommit master
```
- 해당 프로세스는 IntelliJ 작업에 영향을 주지 않음. 작업 이후, GUI 를 통하여 `pull` / `push` / `commit` 등을 하면 원래 repository 에 처리됨

#### Trouble Shooting

codecommit 원격 브랜치에 `git push codecommit master` 시에 에러 메세지에 `non-fast-forward` 라는 문구가 나타나면 아래와 같이 처리합니다.
```
# 뒤에 붙는 브랜치 명을 제외합니다.
$ > git push codecommit 
```
- 원인 : 커밋 트리가 달라서 병합이 안됨 > 어차피 `codecommit` 은 완성된 소스를 배포하기 위해 걸치는 절차이므로 현재 ion 브랜치가 최신이라면 무조건 덮어쓰면 해결됨

#### 계정정보

```
ID : prd_devops@ion-at-987692723403
PW : 5dyn5TEBHHqENrjnP2F2jZlC/FQadypZCqv7amYtigs=
```

#### History

- 2017.10.12 - 초안 작성 