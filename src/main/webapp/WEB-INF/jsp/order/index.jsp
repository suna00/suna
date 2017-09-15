<%--
  Created by IntelliJ IDEA.
  User: yoonseonwoong
  Date: 2017. 9. 11.
  Time: PM 3:10
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=utf-8" language="java" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="tempOrder" value="${tempOrderData}"/>

<!DOCTYPE html>
<html lang="ko">
<head>
    <title>주문/결제</title>
    <meta name="Keywords" content="교육할인스토어">
    <meta name="Description" content="오직 대학생을 위한 할인프로그램, 대학생 와이군 서식중">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <link rel="stylesheet" href="/styles/style.css">
    <script src="/script/jquery.min.js"></script>
    <script src="/script/plugins.js"></script>
    <script src="/script/ui.js"></script>
</head>
<body>

<div class="sknav">
    <a href="#contents" class="sknavi">콘텐츠 바로가기</a>
</div>

<!-- wrap -->
<div id="wrap">
    <!-- container -->
    <div id="container">

        <!-- top banner -->
        <div class="top_banner">
            <div class="in_container">
                top banner
            </div>
        </div>
        <!-- // top banner -->

        <!-- header -->
        <header id="header">
            <!-- util -->
            <div class="util">
                <div class="in_container">
                    <ul>
                        <li><a href="javascript:void(0);" class="favorite"><span>즐겨찾기</span></a></li>
                        <li><a href="javascript:void(0);" class="app_download"><span>앱다운로드</span></a></li>
                    </ul>

                    <ul>
                        <li><a href="javascript:void(0);"><span>로그인</span></a></li>
                        <li><a href="javascript:void(0);" class="mypage"><span>MY PAGE</span></a>
                            <div class="util_nav">
                                <div class="util_nav_tit"><a href="javascript:void(0);"><span>MY PAGE</span></a></div>
                                <ul>
                                    <li><a href="javascript:void(0);"><span>주문/배송조회</span></a></li>
                                    <li><a href="javascript:void(0);"><span>쿠폰</span></a></li>
                                    <li><a href="javascript:void(0);"><span>위시리스트</span></a></li>
                                </ul>
                            </div>
                        </li>
                        <li><a href="javascript:void(0);" class="cart"><span>장바구니</span><span class="cnt">3</span></a></li>
                        <li><a href="javascript:void(0);" class="cs"><span>고객센터</span></a>
                            <div class="util_nav">
                                <div class="util_nav_tit"><a href="javascript:void(0);"><span>고객센터</span></a></div>
                                <ul>
                                    <li><a href="javascript:void(0);"><span>FAQ</span></a></li>
                                    <li><a href="javascript:void(0);"><span>1:1문의</span></a></li>
                                    <li><a href="javascript:void(0);"><span>공지사항</span></a></li>
                                    <li><span class="tel">02)<br>398-8880</span></li>
                                </ul>
                            </div>
                        </li>
                    </ul>
                </div>
            </div>
            <!-- // util -->

            <!-- other -->
            <div class="other">
                <div class="in_container">

                    <div class="other_container">

                        <div class="pop_keyword">
                            <ul>
                                <li>
                                    <a href="javascript:void(0);"><span class="rank">1</span><span class="keyword">노트북</span></a>
                                    <div class="pop_keyword_list">
                                        <p class="tit">인기 검색어</p>
                                        <ul>
                                            <li><a href="javascript:void(0);"><span class="rank">1</span><span class="keyword">노트북</span><span
                                                    class="new">NEW</span></a></li>
                                            <li><a href="javascript:void(0);"><span class="rank">2</span><span class="keyword">에어컨</span></a></li>
                                            <li><a href="javascript:void(0);"><span class="rank">3</span><span class="keyword">김치냉장고</span></a></li>
                                            <li><a href="javascript:void(0);"><span class="rank">4</span><span class="keyword">여성샌들</span></a></li>
                                            <li><a href="javascript:void(0);"><span class="rank">5</span><span class="keyword">남성샌들</span></a></li>
                                        </ul>
                                    </div>
                                </li>
                            </ul>
                        </div>

                        <div class="logo_srch">
                            <div class="logo_srch_box">
                                <div class="logo"><a href="javascript:void(0);"><img src="/image/sample/img_ygoon_logo.gif" alt="YGOON 교육할인 스토어"></a>
                                </div>
                                <div class="srch">
                                    <form action="#">
                                        <fieldset>
                                            <legend>검색</legend>
                                            <div class="field_wrap">
                                                <label for="keyword" class="blind">검색어입력</label>
                                                <input type="text" class="txt" title="통합검색" id="keyword" value="LG전자 체인지업 페스티벌">
                                                <a href="javascript:void(0);" class="btn_srch"><span class="blind">검색</span></a>
                                            </div>
                                        </fieldset>
                                    </form>
                                </div>
                            </div>
                        </div>

                        <div class="banner_area">
                            <ul>
                                <li><a href="javascript:void(0);"><img src="http://via.placeholder.com/152x60/cccccc/333333?text=1"
                                                                       alt="교육할인 스토어 YGOON"></a></li>
                                <li class="hide"><a href="javascript:void(0);"><img src="http://via.placeholder.com/152x60/cccccc/333333?text=2"
                                                                                    alt="교육할인 스토어 YGOON"></a></li>
                                <li class="hide"><a href="javascript:void(0);"><img src="http://via.placeholder.com/152x60/cccccc/333333?text=3"
                                                                                    alt="교육할인 스토어 YGOON"></a></li>
                                <li class="hide"><a href="javascript:void(0);"><img src="http://via.placeholder.com/152x60/cccccc/333333?text=4"
                                                                                    alt="교육할인 스토어 YGOON"></a></li>
                                <li class="hide"><a href="javascript:void(0);"><img src="http://via.placeholder.com/152x60/cccccc/333333?text=5"
                                                                                    alt="교육할인 스토어 YGOON"></a></li>
                            </ul>
                            <div class="banner_cnt">
                                <span class="cur">1</span>
                                <span class="tot">/5</span>
                            </div>
                            <div class="banner_arr">
                                <a href="javascript:void(0);" class="prev">이전</a>
                                <a href="javascript:void(0);" class="next">다음</a>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
            <!-- // other -->

            <!-- nav -->
            <nav class="nav">
                <div class="in_container">

                    <!-- view_all_cat -->
                    <div class="view_all_cat">
                        <a href="javascript:void(0);" class="btn_view_all"><span>카테고리 전체</span></a>
                    </div>
                    <!-- // view_all_cat -->

                    <!-- GNB 스페셜 -->
                    <div class="nav_special">
                        <ul>
                            <li><a href="javascript:void(0);">신상품</a></li>
                            <li><a href="javascript:void(0);" class="mont">HOT DEAL</a></li>
                            <li><a href="javascript:void(0);" class="mont">BEST50</a></li>
                            <li><a href="javascript:void(0);">기획전</a></li>
                        </ul>

                        <ul>
                            <li><a href="javascript:void(0);"><span>교육&middot;문화 제휴</span></a>
                                <div class="spon_subnav">
                                    <div class="spon_box">
                                        <div class="spon_list">
                                            <ul>
                                                <li><a href="javascript:void(0);">영화 싸게 보기</a></li>
                                                <li><a href="javascript:void(0);">대학로 살리기 프로젝트</a></li>
                                                <li><a href="javascript:void(0);">뮤지컬 연극</a></li>
                                                <li><a href="javascript:void(0);">놀이동산</a></li>
                                                <li><a href="javascript:void(0);">대학생도서 혜택</a></li>
                                                <li><a href="javascript:void(0);">대학생 중고도서장터</a></li>
                                            </ul>
                                        </div>
                                    </div>
                                </div>
                            </li>
                            <li><a href="javascript:void(0);"><span class="mont">YGOON 제휴</span></a>
                                <div class="spon_subnav">
                                    <div class="spon_box">
                                        <div class="spon_list">
                                            <ul>
                                                <li><a href="javascript:void(0);">와이군 형제들</a></li>
                                                <li><a href="javascript:void(0);">비행기 싸게 타기</a></li>
                                                <li><a href="javascript:void(0);">자격증 싸게 따기</a></li>
                                                <li><a href="javascript:void(0);">오프라인 할인 혜택</a></li>
                                            </ul>
                                        </div>
                                    </div>
                                </div>
                            </li>
                        </ul>
                    </div>
                    <!-- // GNB 스페셜 -->
                </div>
            </nav>
            <!-- // nav -->

            <header id="header_fixed" class="header_fixed">
                <div class="in_container">
                    <div class="header_fixed_wrap">
                        <div class="fixed_view_cat">
                            <a href="javascript:void(0);" class="fixed_view_all"><span>카테고리 전체</span></a>
                        </div>
                        <div class="fixed_logo"><a href="javascript:void(0);"><img src="/image/common/img_header_fixed_logo.png" alt="YGOON 교육할인스토어"></a>
                        </div>
                        <div class="fixed_srch">
                            <div class="fixed_srch_box">
                                <form action="#">
                                    <fieldset>
                                        <legend>검색</legend>
                                        <div class="field_wrap">
                                            <label for="keyword" class="blind">검색어입력</label>
                                            <input type="text" class="txt" title="통합검색" id="searchKeyword" value="LG전자 체인지업 페스티벌">
                                            <a href="javascript:void(0);" class="btn_srch"><span class="blind">검색</span></a>
                                        </div>
                                    </fieldset>
                                </form>
                            </div>
                        </div>
                        <div class="fixed_special">
                            <ul>
                                <li><a href="javascript:void(0);"><span>신상품</span></a></li>
                                <li><a href="javascript:void(0);" class="mont"><span>HOT DEAL</span></a></li>
                                <li><a href="javascript:void(0);" class="mont"><span>BEST50</span></a></li>
                                <li><a href="javascript:void(0);"><span>기획전</span></a></li>
                            </ul>

                            <ul>
                                <li><a href="javascript:void(0);"><span>교육&middot;문화 제휴</span></a>
                                    <div class="spon_subnav">
                                        <div class="spon_box">
                                            <div class="spon_list">
                                                <ul>
                                                    <li><a href="javascript:void(0);">영화 싸게 보기</a></li>
                                                    <li><a href="javascript:void(0);">대학로 살리기 프로젝트</a></li>
                                                    <li><a href="javascript:void(0);">뮤지컬 연극</a></li>
                                                    <li><a href="javascript:void(0);">놀이동산</a></li>
                                                    <li><a href="javascript:void(0);">대학생도서 혜택</a></li>
                                                    <li><a href="javascript:void(0);">대학생 중고도서장터</a></li>
                                                </ul>
                                            </div>
                                        </div>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);"><span class="mont">YGOON 제휴</span></a>
                                    <div class="spon_subnav">
                                        <div class="spon_box">
                                            <div class="spon_list">
                                                <ul>
                                                    <li><a href="javascript:void(0);">와이군 형제들</a></li>
                                                    <li><a href="javascript:void(0);">비행기 싸게 타기</a></li>
                                                    <li><a href="javascript:void(0);">자격증 싸게 따기</a></li>
                                                    <li><a href="javascript:void(0);">오프라인 할인 혜택</a></li>
                                                </ul>
                                            </div>
                                        </div>
                                    </div>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </header>

            <!-- 카테고리 전체 -->
            <div class="view_all_cat_box">
                <ul>
                    <li>
                        <div class="view_all_cat_inbox">
                            <p><a href="javascript:void(0);">디지털/가전</a></p>
                            <ul>
                                <li><a href="javascript:void(0);">노트북</a>
                                    <div class="dp3_nav" style="width:150px;">
                                        <ul>
                                            <li><a href="javascript:void(0);">삼성노트북</a></li>
                                            <li><a href="javascript:void(0);">LG노트북</a></li>
                                            <li><a href="javascript:void(0);">맥북&amp;HP노트북</a></li>
                                            <li><a href="javascript:void(0);">노트북</a></li>
                                            <li><a href="javascript:void(0);">태블릿</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">냉장고/김치냉장고/세탁기</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">냉장고</a></li>
                                            <li><a href="javascript:void(0);">김치냉장고</a></li>
                                            <li><a href="javascript:void(0);">세탁기</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">TV/음향기기</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">TV</a></li>
                                            <li><a href="javascript:void(0);">사운드바/헤드폰/이어폰</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">PC&amp;모니터</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">데스크탑</a></li>
                                            <li><a href="javascript:void(0);">모니터</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">PC주변기기</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">키보드/마우스</a></li>
                                            <li><a href="javascript:void(0);">저장장치/PC주변기기</a></li>
                                            <li><a href="javascript:void(0);">프린터</a></li>
                                            <li><a href="javascript:void(0);">잉크/토너</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">카메라</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">DSLR</a></li>
                                            <li><a href="javascript:void(0);">컴팩트</a></li>
                                            <li><a href="javascript:void(0);">캠코더/액션캠</a></li>
                                            <li><a href="javascript:void(0);">카메라 주변기기</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">주방/생활가전</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">전기밥솥</a></li>
                                            <li><a href="javascript:void(0);">믹서/커피머신</a></li>
                                            <li><a href="javascript:void(0);">토스터기/전기포트</a></li>
                                            <li><a href="javascript:void(0);">청소기/다리미</a></li>
                                            <li><a href="javascript:void(0);">전자레인지/오븐</a></li>
                                            <li><a href="javascript:void(0);">가스/전기레인지</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">뷰티/건강</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">면도기</a></li>
                                            <li><a href="javascript:void(0);">드라이기/고데기</a></li>
                                            <li><a href="javascript:void(0);">공기청정기</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">계절가전</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">에어컨</a></li>
                                            <li><a href="javascript:void(0);">에어워셔/가습기</a></li>
                                            <li><a href="javascript:void(0);">제습기</a></li>
                                            <li><a href="javascript:void(0);">선풍기/써큘레이터</a></li>
                                            <li><a href="javascript:void(0);">여름소형</a></li>
                                            <li><a href="javascript:void(0);">겨울소형</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">리퍼비시관</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">삼성노트북(리퍼)</a></li>
                                            <li><a href="javascript:void(0);">LG노트북(리퍼)</a></li>
                                            <li><a href="javascript:void(0);">애플(리퍼)</a></li>
                                            <li><a href="javascript:void(0);">HP/레노버/외산(리퍼)</a></li>
                                            <li><a href="javascript:void(0);">데스크탑(리퍼)</a></li>
                                            <li><a href="javascript:void(0);">가전/생활용품(리퍼)</a></li>
                                            <li><a href="javascript:void(0);">휴대폰(리퍼/중고)</a></li>
                                        </ul>
                                    </div>
                                </li>
                            </ul>
                        </div>
                    </li>
                    <li>
                        <div class="view_all_cat_inbox">
                            <p><a href="javascript:void(0);">스포츠/레저</a></p>
                            <ul>
                                <li><a href="javascript:void(0);">스포츠용품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">등산용품</a></li>
                                            <li><a href="javascript:void(0);">캠핑용품</a></li>
                                            <li><a href="javascript:void(0);">구기용품</a></li>
                                            <li><a href="javascript:void(0);">라이딩용품</a></li>
                                            <li><a href="javascript:void(0);">헬스용품</a></li>
                                            <li><a href="javascript:void(0);">수영용품</a></li>
                                            <li><a href="javascript:void(0);">방한용품</a></li>
                                            <li><a href="javascript:void(0);">전자기기</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">스포츠의류</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">상의</a></li>
                                            <li><a href="javascript:void(0);">하의</a></li>
                                            <li><a href="javascript:void(0);">아우터</a></li>
                                            <li><a href="javascript:void(0);">트레이닝복</a></li>
                                            <li><a href="javascript:void(0);">이너웨어</a></li>
                                            <li><a href="javascript:void(0);">키즈웨어</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">스포츠신발</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">런닝/운동화</a></li>
                                            <li><a href="javascript:void(0);">등산/트래킹화</a></li>
                                        </ul>
                                    </div>
                                </li>
                            </ul>
                        </div>
                        <div class="view_all_cat_inbox">
                            <p><a href="javascript:void(0);">골프</a></p>
                            <ul>
                                <li><a href="javascript:void(0);">골프클럽</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">풀세트</a></li>
                                            <li><a href="javascript:void(0);">드라이버</a></li>
                                            <li><a href="javascript:void(0);">우드</a></li>
                                            <li><a href="javascript:void(0);">유틸리티</a></li>
                                            <li><a href="javascript:void(0);">아이언세트</a></li>
                                            <li><a href="javascript:void(0);">웨지</a></li>
                                            <li><a href="javascript:void(0);">피터</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">골프용품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">골프공</a></li>
                                            <li><a href="javascript:void(0);">골프화</a></li>
                                            <li><a href="javascript:void(0);">골프벨트</a></li>
                                            <li><a href="javascript:void(0);">골프장갑</a></li>
                                            <li><a href="javascript:void(0);">골프가방</a></li>
                                            <li><a href="javascript:void(0);">필드용품</a></li>
                                            <li><a href="javascript:void(0);">연습용품</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">골프의류</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">상의</a></li>
                                            <li><a href="javascript:void(0);">하의</a></li>
                                        </ul>
                                    </div>
                                </li>
                            </ul>
                        </div>
                    </li>
                    <li>
                        <div class="view_all_cat_inbox">
                            <p><a href="javascript:void(0);">의류/잡화</a></p>
                            <ul>
                                <li><a href="javascript:void(0);">가방/잡화</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">백팩/크로스백</a></li>
                                            <li><a href="javascript:void(0);">핸드백</a></li>
                                            <li><a href="javascript:void(0);">노트북/서류 가방</a></li>
                                            <li><a href="javascript:void(0);">여행용가방</a></li>
                                            <li><a href="javascript:void(0);">지갑/벨트</a></li>
                                            <li><a href="javascript:void(0);">모자/시즌소품</a></li>
                                            <li><a href="javascript:void(0);">선글라스/안경테</a></li>
                                            <li><a href="javascript:void(0);">우산/양산</a></li>
                                            <li><a href="javascript:void(0);">스타킹/양말</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">시계/주얼리</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">브랜드시계</a></li>
                                            <li><a href="javascript:void(0);">패션시계</a></li>
                                            <li><a href="javascript:void(0);">쥬얼리</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">신발</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">운동화</a></li>
                                            <li><a href="javascript:void(0);">샌들/슬리퍼</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">남성의류</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">상의</a></li>
                                            <li><a href="javascript:void(0);">하의</a></li>
                                            <li><a href="javascript:void(0);">아우터</a></li>
                                            <li><a href="javascript:void(0);">니트/집업</a></li>
                                            <li><a href="javascript:void(0);">남성언더웨어</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">여성의류</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">티셔츠</a></li>
                                            <li><a href="javascript:void(0);">아우터</a></li>
                                            <li><a href="javascript:void(0);">스커트/바지</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">해외명품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">피혁잡화</a></li>
                                            <li><a href="javascript:void(0);">시즌소품</a></li>
                                        </ul>
                                    </div>
                                </li>
                            </ul>
                        </div>
                        <div class="view_all_cat_inbox">
                            <p><a href="javascript:void(0);">뷰티</a></p>
                            <ul>
                                <li><a href="javascript:void(0);">향수</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">여성향수</a></li>
                                            <li><a href="javascript:void(0);">남성향수</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">기초화장품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">마스크/팩</a></li>
                                            <li><a href="javascript:void(0);">스킨/로션</a></li>
                                            <li><a href="javascript:void(0);">에센스/오일</a></li>
                                            <li><a href="javascript:void(0);">선블록/미스트</a></li>
                                            <li><a href="javascript:void(0);">크림</a></li>
                                            <li><a href="javascript:void(0);">아이케어</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">메이크업/클렌징</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">클렌징</a></li>
                                            <li><a href="javascript:void(0);">베이스/BB크림</a></li>
                                            <li><a href="javascript:void(0);">팩트/파우더</a></li>
                                            <li><a href="javascript:void(0);">립/치크메이크업</a></li>
                                            <li><a href="javascript:void(0);">아이메이크업</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">바디/헤어/핸드</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">바디워시</a></li>
                                            <li><a href="javascript:void(0);">바디로션</a></li>
                                            <li><a href="javascript:void(0);">핸드/풋케어</a></li>
                                            <li><a href="javascript:void(0);">헤어워시</a></li>
                                            <li><a href="javascript:void(0);">헤어에센스</a></li>
                                            <li><a href="javascript:void(0);">바디미용용품</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">유기농</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">유기농 스킨케어</a></li>
                                            <li><a href="javascript:void(0);">유아 스킨케어</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">남성화장품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">기초케어</a></li>
                                            <li><a href="javascript:void(0);">BB/클렌징</a></li>
                                        </ul>
                                    </div>
                                </li>
                            </ul>
                        </div>
                    </li>
                    <li>
                        <div class="view_all_cat_inbox">
                            <p><a href="javascript:void(0);">주방/생활/유아</a></p>
                            <ul>
                                <li><a href="javascript:void(0);">주방용품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">식기/홈셋트</a></li>
                                            <li><a href="javascript:void(0);">냄비/후라이팬</a></li>
                                            <li><a href="javascript:void(0);">보관/밀폐용기</a></li>
                                            <li><a href="javascript:void(0);">조리도구</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">건강/생활용품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">안마기/찜질기</a></li>
                                            <li><a href="javascript:void(0);">건강측정기</a></li>
                                            <li><a href="javascript:void(0);">건강악세사리</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">위생/욕실용품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">화장지/티슈</a></li>
                                            <li><a href="javascript:void(0);">욕실용품</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">유아용품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">유모차/카시트</a></li>
                                            <li><a href="javascript:void(0);">아기띠/외출용품</a></li>
                                            <li><a href="javascript:void(0);">실내용품</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">수납/공구</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">공구/철물/안전</a></li>
                                            <li><a href="javascript:void(0);">생활악세사리</a></li>
                                        </ul>
                                    </div>
                                </li>
                            </ul>
                        </div>
                        <div class="view_all_cat_inbox">
                            <p><a href="javascript:void(0);">식품</a></p>
                            <ul>
                                <li><a href="javascript:void(0);">건강/다이어트</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">홍삼/건강즙</a></li>
                                            <li><a href="javascript:void(0);">건강기능식품</a></li>
                                            <li><a href="javascript:void(0);">다이어트식</a></li>
                                            <li><a href="javascript:void(0);">헬스/뷰티푸드</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">가공식품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">즉석조리식품</a></li>
                                            <li><a href="javascript:void(0);">냉동보관식품</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">음료/간식</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">커피/차</a></li>
                                            <li><a href="javascript:void(0);">과자/아이스크림</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">신선식품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">수산물</a></li>
                                            <li><a href="javascript:void(0);">김치/장</a></li>
                                            <li><a href="javascript:void(0);">농산물</a></li>
                                        </ul>
                                    </div>
                                </li>
                            </ul>
                        </div>
                    </li>
                    <li>
                        <div class="view_all_cat_inbox">
                            <p><a href="javascript:void(0);">가구/홈데코</a></p>
                            <ul>
                                <li><a href="javascript:void(0);">학생가구</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">책상</a></li>
                                            <li><a href="javascript:void(0);">의자</a></li>
                                            <li><a href="javascript:void(0);">책장</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">거실/주방가구</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">거실가구</a></li>
                                            <li><a href="javascript:void(0);">주방가구</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">침실/수납가구</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">침대</a></li>
                                            <li><a href="javascript:void(0);">장롱/옷장</a></li>
                                            <li><a href="javascript:void(0);">화장대/수납가구</a></li>
                                            <li><a href="javascript:void(0);">장식가구/행거</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">침구/카페트</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">침구/이불</a></li>
                                            <li><a href="javascript:void(0);">카페트</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">조명/소품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">조명/시계/액자</a></li>
                                            <li><a href="javascript:void(0);">디자인용품</a></li>
                                            <li><a href="javascript:void(0);">시즌용품</a></li>
                                        </ul>
                                    </div>
                                </li>
                            </ul>
                        </div>
                        <div class="view_all_cat_inbox">
                            <p><a href="javascript:void(0);">e-쿠폰/문구/악기</a></p>
                            <ul>
                                <li><a href="javascript:void(0);">e-쿠폰/교육</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">교육/여행/문화</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">문구/사무/생활용품</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">복사용지/지류</a></li>
                                            <li><a href="javascript:void(0);">필기구</a></li>
                                            <li><a href="javascript:void(0);">사무용품</a></li>
                                            <li><a href="javascript:void(0);">식음료</a></li>
                                            <li><a href="javascript:void(0);">화일바인더</a></li>
                                            <li><a href="javascript:void(0);">사무기기</a></li>
                                            <li><a href="javascript:void(0);">미용위생</a></li>
                                            <li><a href="javascript:void(0);">학용품/채육용품</a></li>
                                            <li><a href="javascript:void(0);">디자인/화방</a></li>
                                            <li><a href="javascript:void(0);">금고/가구/잡화</a></li>
                                            <li><a href="javascript:void(0);">산업용품</a></li>
                                            <li><a href="javascript:void(0);">생활용품</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">DIY/공예</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">미싱/취미</a></li>
                                        </ul>
                                    </div>
                                </li>
                                <li><a href="javascript:void(0);">악기</a>
                                    <div class="dp3_nav">
                                        <ul>
                                            <li><a href="javascript:void(0);">디지털피아노</a></li>
                                            <li><a href="javascript:void(0);">신디사이저</a></li>
                                        </ul>
                                    </div>
                                </li>
                            </ul>
                        </div>
                    </li>
                </ul>
            </div>
            <!-- 카테고리 전체 -->
        </header>
        <!-- // header -->

        <!-- content_wrap -->
        <main id="content_wrap">
            <section id="contents">

                <!-- cart_wrap -->
                <div class="cart_wrap">
                    <div class="in_container">

                        <!-- cart_header -->
                        <div class="cart_header">
                            <div class="cart_tit">
                                <h2>주문/결제</h2>
                            </div>
                            <div class="cart_proc">
                                <ul>
                                    <li class="proc1"><span>장바구니</span></li>
                                    <li class="proc2 on"><span>주문/결제</span></li>
                                    <li class="proc3"><span>주문완료</span></li>
                                </ul>
                            </div>
                        </div>
                        <!-- // cart_header -->

                        <!-- cart_box -->
                        <div class="cart_box">
                            <h3>결제 예정 상품</h3>
                            <!-- cart_goods_list -->
                            <div class="cart_goods_list">
                                <table>
                                    <colgroup>
                                        <col style="width:auto">
                                        <col style="width:145px;">
                                        <col style="width:230px;">
                                        <col style="width:97px;">
                                        <col style="width:252px;">
                                    </colgroup>
                                    <thead>
                                    <tr>
                                        <th scope="col">상품정보</th>
                                        <th scope="col">수량</th>
                                        <th scope="col">상품금액</th>
                                        <th scope="col">배송비</th>
                                        <th scope="col">주문금액</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach var="tempOrderDeliveryPrice" items="${tempOrder.referencedTempOrderDeliveryPrice.items}"
                                               varStatus="tempOrderDeliveryPriceStatus">
                                        <c:forEach var="tempOrderProduct" items="${tempOrderDeliveryPrice.referencedTempOrderProduct}"
                                                   varStatus="tempOrderProductStatus">
                                            <c:set var="product" value="${tempOrderProduct.productId.item}"/>
                                            <tr>
                                                <td scope="row" class="info_td">
                                                    <div class="thumb">
                                                        <img src="/image/sample/thumb_sample_600_2.jpg"
                                                             alt="LG전자 ★LG전자&amp;교육할인공동기획★[LG전자] 초경량그램14 14ZD970-EX3YL">
                                                        <div class="soldout_badge">품절</div>
                                                    </div>
                                                    <div class="info">
                                                        <p class="nm">${tempOrderProduct.name}</p>
                                                        <p class="opt">${tempOrderProduct.calculateItem.baseOptionItemName}</p>

                                                        <!-- 2017-08-28 -->
                                                        <c:if test="${fn:length(tempOrderProduct.referencedTempOrderProductItem) > 0}">
                                                            <div class="add_opt">
                                                                <p>추가옵션</p>
                                                                <ul>
                                                                    <c:forEach var="tempOrderProductItem"
                                                                               items="${tempOrderProduct.referencedTempOrderProductItem}"
                                                                               varStatus="tempOrderProductItemStatus">
                                                                        <li>
                                                                            <span class="nm">${tempOrderProductItem.addOptionItemId.label} / ${tempOrderProductItem.quantity}개</span>
                                                                            <span class="price">
                                                                                <span class="mont"><fmt:formatNumber
                                                                                        value="${tempOrderProductItem.addOptionItemId.item.addPrice}"
                                                                                        pattern="#,###"/></span>
                                                                                <span class="unit">원</span>
                                                                            </span>
                                                                        </li>
                                                                    </c:forEach>
                                                                </ul>
                                                            </div>
                                                        </c:if>
                                                        <!-- 2017-08-28 -->

                                                        <!-- 2017-08-28 -->
                                                            <%--<div class="timesale_badge">--%>
                                                            <%--<span>타임세일 마감</span>--%>
                                                            <%--<span class="mont">07 : 30 : 31</span>--%>
                                                            <%--</div>--%>
                                                        <!-- // 2017-08-28 -->
                                                    </div>
                                                </td>
                                                <td class="brd_l">${tempOrderProduct.calculateItem.quantity}</td>
                                                <td>
                                                            <span class="goods_price">
                                                                <span class="mont"><fmt:formatNumber
                                                                        value="${tempOrderProduct.calculateItem.productPrice}"
                                                                        pattern="#,###"/></span><span class="unit">원</span>
                                                                <c:if test="${tempOrderProduct.calculateItem.totalAddOptionPrice > 0}">
                                                                    <span class="mont">+ <fmt:formatNumber
                                                                            value="${tempOrderProduct.calculateItem.totalAddOptionPrice}"
                                                                            pattern="#,###"/></span><span class="unit">원</span>
                                                                    <span class="opt_add">(옵션추가)</span>
                                                                </c:if>
                                                            </span>
                                                </td>
                                                <c:if test="${tempOrderProductStatus.index == 0}">
                                                    <c:if test="${tempOrderDeliveryPrice.deliveryPrice > 0}">
                                                        <td rowspan="${fn:length(tempOrderProduct)}"><fmt:formatNumber
                                                                value="${tempOrderDeliveryPrice.deliveryPrice}" pattern="#,###"/>원
                                                        </td>
                                                    </c:if>
                                                    <c:if test="${tempOrderDeliveryPrice.deliveryPrice == 0}">
                                                        <td>무료</td>
                                                    </c:if>
                                                </c:if>
                                                <td>
                                                            <span class="tot_price">
                                                                <span class="mont"><fmt:formatNumber
                                                                        value="${tempOrderProduct.calculateItem.orderPrice}" pattern="#,###"/></span>
                                                                <span class="unit">원</span>
                                                            </span>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                            <!-- // cart_goods_list -->

                            <div class="cart_tot_box cnt3">
                                <ul>
                                    <li>
                                        <span class="tit">총 상품금액</span>
                                        <span class="price">
                                                <span class="mont"><fmt:formatNumber value="${tempOrder.item.totalProductPrice}"
                                                                                     pattern="#,###"/></span>
                                                <span class="unit">원</span>
                                            </span>
                                    </li>
                                    <li>
                                        <span class="tit">총 배송비</span>
                                        <span class="price">
                                                <span class="mont"><fmt:formatNumber value="${tempOrder.item.totalDeliveryPrice}"
                                                                                     pattern="#,###"/></span>
                                                <span class="unit">원</span>
                                            </span>
                                    </li>
                                    <li>
                                        <span class="tit">총 주문금액</span>
                                        <span class="price">
                                                <span class="mont"><fmt:formatNumber value="${tempOrder.item.totalOrderPrice}"
                                                                                     pattern="#,###"/></span>
                                                <span class="unit">원</span>
                                            </span>
                                    </li>
                                </ul>
                            </div>

                            <!-- order info -->
                            <div class="ord_info">
                                <h3>주문자 정보</h3>
                                <div class="ord_info_box">
                                    <table>
                                        <colgroup>
                                            <col style="width:148px;">
                                            <col style="width:auto;">
                                            <col style="width:148px;">
                                            <col style="width:auto;">
                                        </colgroup>
                                        <tr>
                                            <th>이름</th>
                                            <td>홍길동 (the***@na***)</td>
                                            <th>휴대폰번호</th>
                                            <td>010-1234-5678</td>
                                        </tr>
                                    </table>
                                </div>
                            </div>
                            <!-- // order info -->

                            <!-- order info -->
                            <div class="ord_info">
                                <h3>배송지정보</h3>
                                <div class="ord_info_box">
                                    <table>
                                        <colgroup>
                                            <col style="width:148px;">
                                            <col style="width:auto;">
                                            <col style="width:148px;">
                                            <col style="width:auto;">
                                        </colgroup>
                                        <tr>
                                            <th>배송지 선택</th>
                                            <td colspan="3">
                                                    <span class="in_field">
                                                        <input type="radio" class="rdo" id="address1">
                                                        <label for="address1">기본 배송지</label>
                                                    </span>
                                                <span class="in_field">
                                                        <input type="radio" class="rdo" id="address2">
                                                        <label for="address2">새 배송지</label>
                                                    </span>

                                                <span class="in_field">
                                                        <select class="sel">
                                                            <option>최근배송지</option>
                                                        </select>
                                                        <a href="javascript:void(0);" class="btn brd btn_modal" data-src="addr_list"><span>주소록</span></a>
                                                    </span>
                                            </td>
                                        </tr>
                                        <tr>
                                            <th>받는 분</th>
                                            <td>
                                                    <span class="in_field">
                                                        <input type="text" class="txt">
                                                    </span>
                                                <span class="in_field">
                                                        <input type="checkbox" id="chk1">
                                                        <label for="chk1">주문자와 동일</label>
                                                    </span>
                                            </td>
                                            <th>배송지명</th>
                                            <td><input type="text" class="txt"></td>
                                        </tr>
                                        <tr>
                                            <th rowspan="2">주소</th>
                                            <td colspan="3" class="addr_basic">
                                                    <span class="in_field">
                                                       <input type="text" class="txt">
                                                       <a href="javascript:void(0);" class="btn brd" onclick="getTemporderData()"><span>주소찾기</span></a>
                                                    </span>

                                                <span class="in_field">
                                                       <input type="checkbox" id="chk2">
                                                       <label for="chk2">주소록 추가</label>
                                                    </span>
                                                <span class="in_field">
                                                       <input type="checkbox" id="chk3">
                                                       <label for="chk3">기본 배송지</label>
                                                    </span>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td colspan="3" class="addr_etc">
                                                    <span class="in_field">
                                                        <input type="text" class="txt">
                                                    </span>
                                                <span class="in_field">
                                                        <input type="text" class="txt">
                                                    </span>
                                            </td>
                                        </tr>
                                        <tr>
                                            <th>휴대폰 번호</th>
                                            <td colspan="3">
                                                <select class="sel tel">
                                                    <option>010</option>
                                                </select>
                                                <input type="text" class="txt tel">
                                                <input type="text" class="txt tel">
                                            </td>
                                        </tr>
                                        <tr>
                                            <th>배송메시지</th>
                                            <td colspan="3">
                                                <select class="sel deli_msg">
                                                    <option>최근배송지</option>
                                                </select>
                                                <span class="limit">0/100</span>
                                            </td>
                                        </tr>
                                    </table>
                                </div>
                            </div>
                            <!-- // order info -->

                            <!-- order_footer -->
                            <div class="ord_footer">
                                <div class="sale_method_info">
                                    <div class="sale_info ord_info">
                                        <h3>할인적용</h3>
                                        <div class="ord_info_box">
                                            <table>
                                                <colgroup>
                                                    <col style="width:148px;">
                                                    <col style="width:auto">
                                                </colgroup>
                                                <tr>
                                                    <th>할인쿠폰
                                                        <a href="javascript:void(0);" class="tooltip"><img src="/image/cart/btn_label_question.png"
                                                                                                           alt=""></a>
                                                        <div class="tooltip_box coupon_desc">
                                                            <p>
                                                                할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰할인쿠폰</p>
                                                        </div>
                                                    </th>
                                                    <td>
                                                        <div class="coupon_info">
                                                            <a href="javascript:void(0);" class="btn sml blue btn_modal" data-src="coupon_list"><span>쿠폰 조회/적용</span></a>
                                                            <span>사용가능 쿠폰 : <strong>2장</strong> / 보유쿠폰 : <strong>10장</strong></span>
                                                        </div>
                                                        <div class="coupon_info">
                                                            <input type="checkbox" class="chk" id="chk4">
                                                            <label for="chk4">쿠폰 최대 할인적용</label>
                                                        </div>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <th>복지포인트
                                                        <a href="javascript:void(0);" class="tooltip"><img src="/image/cart/btn_label_question.png"
                                                                                                           alt=""></a>
                                                        <div class="tooltip_box pt1_desc">
                                                            <p>복지포인트복지포인트복지포인트복지포인트복지포인트복지포인트복지포인트복지포인트복지포인트복지포인트복지포인트복지포인트복지포인트복지포인트복지포인트복지포인트</p>
                                                        </div>
                                                    </th>
                                                    <td>
                                                        <input type="text" class="txt pt"> 원 / <span class="pt_info">150,000P</span>
                                                        <a href="javascript:void(0);" class="btn sml blue brd"><span>전체사용</span></a>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <th>Y포인트
                                                        <a href="javascript:void(0);" class="tooltip"><img src="/image/cart/btn_label_question.png"
                                                                                                           alt=""></a>
                                                        <div class="tooltip_box pt2_desc">
                                                            <p>Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트Y포인트</p>
                                                        </div>
                                                    </th>
                                                    <td>
                                                        <input type="text" class="txt pt"> 원 / <span class="pt_info">150,000P</span>
                                                        <a href="javascript:void(0);" class="btn sml blue brd"><span>전체사용</span></a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p class="pt_srch">
                                                복지포인트, Y포인트 상세조회는 <a href="javascript:void(0);">MYPAGE &gt; 쇼핑혜택</a>에서 확인하실 수 있습니다.
                                                <a href="javascript:void(0);" class="btn brd"><span>내 쇼핑혜택 이동</span></a>
                                            </p>
                                        </div>
                                    </div>

                                    <div class="method_info ord_info">
                                        <h3>결제수단</h3>
                                        <div class="ord_info_box">
                                            <table>
                                                <colgroup>
                                                    <col style="width:148px;">
                                                    <col style="width:auto">
                                                </colgroup>
                                                <tr>
                                                    <th>결제수단</th>
                                                    <td>
                                                        <div class="payment_method">
                                                            <ul>
                                                                <li>
                                                                    <input type="radio" id="rdo_pay1" name="pay_method" value="100000000000" checked>
                                                                    <label for="rdo_pay1">신용카드</label>
                                                                </li>
                                                                <li>
                                                                    <input type="radio" id="rdo_pay2" name="pay_method" value="010000000000">
                                                                    <label for="rdo_pay2">실시간 계좌이체</label>
                                                                </li>
                                                                <li>
                                                                    <input type="radio" id="rdo_pay3" name="pay_method">
                                                                    <label for="rdo_pay3">무통장입금</label>
                                                                </li>
                                                            </ul>
                                                        </div>
                                                        <div class="payment_method">
                                                            <ul>
                                                                <li>
                                                                    <input type="radio" id="rdo_pay4" name="pay_method" value="000010000000">
                                                                    <label for="rdo_pay4">휴대폰 결제</label>
                                                                </li>
                                                                <li>
                                                                    <input type="radio" id="rdo_pay5" name="pay_method" value="001000000000">
                                                                    <label for="rdo_pay5">가상계좌</label>
                                                                </li>
                                                                <li>
                                                                    <input type="radio" id="rdo_pay6" name="pay_method">
                                                                    <label for="rdo_pay6">간편결제</label>
                                                                </li>
                                                            </ul>
                                                        </div>
                                                    </td>
                                                </tr>
                                                <tr class="without_handbook">
                                                    <th>입금자명</th>
                                                    <td><input type="text" class="txt"></td>
                                                </tr>
                                                <tr class="without_handbook">
                                                    <th>입금계좌</th>
                                                    <td>IBK기업은행 966-000205-01-075 예금주 : ㈜와이티엔</td>
                                                </tr>
                                            </table>
                                        </div>


                                        <!-- 현금영수증 -->
                                        <div class="cash_receipt_box ord_info hide">
                                            <h3>현금영수증 발급 신청</h3>
                                            <p>정보통신망 이용촉진 및 정보보호에 관한 법률 제정으로 인해 주민등록번호 사용이 제한됩니다.</p>
                                            <div class="ord_info_box">
                                                <table>
                                                    <colgroup>
                                                        <col style="width:148px;">
                                                        <col style="width:auto">
                                                    </colgroup>
                                                    <tr>
                                                        <th rowspan="2">용도</th>
                                                        <td>
                                                                <span class="in_field">
                                                                    <input type="radio" id="rec_1" class="rdo rec_type" name="rec_type" checked>
                                                                    <label for="rec_1">개인 소득공제용</label>
                                                                </span>

                                                            <span class="in_field">
                                                                    <input type="radio" id="rec_2" class="rdo rec_type" name="rec_type">
                                                                    <label for="rec_2">사업자 지출증빙용</label>
                                                                </span>

                                                            <span class="in_field">
                                                                    <input type="radio" id="rec_3" class="rdo rec_type" name="rec_type">
                                                                    <label for="rec_3">발급 안함</label>
                                                                </span>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td>

                                                            <select class="sel receipt_type1">
                                                                <option value="" selected>휴대폰번호</option>
                                                                <option value="card_no">현금영수증카드번호</option>
                                                            </select>

                                                            <div class="receipt_box receipt_1">
                                                                <select class="sel tel">
                                                                    <option>010</option>
                                                                </select>
                                                                <input type="text" class="txt tel">
                                                                <input type="text" class="txt tel">
                                                            </div>

                                                            <div class="receipt_box receipt_2 hide">
                                                                <input type="text" class="txt tel">
                                                                <input type="text" class="txt tel">
                                                                <input type="text" class="txt tel">
                                                                <input type="text" class="txt tel">
                                                            </div>

                                                            <select class="sel receipt_type2 hide">
                                                                <option value="" selected>사업자등록번호</option>
                                                                <option value="card_no">현금영수증카드번호</option>
                                                            </select>

                                                            <div class="receipt_box receipt_3 hide">
                                                                <input type="text" class="txt company_no1">
                                                                <input type="text" class="txt company_no2">
                                                                <input type="text" class="txt company_no3">
                                                            </div>

                                                            <div class="receipt_box receipt_4 hide">
                                                                <input type="text" class="txt tel">
                                                                <input type="text" class="txt tel">
                                                                <input type="text" class="txt tel">
                                                                <input type="text" class="txt tel">
                                                            </div>

                                                        </td>
                                                    </tr>
                                                </table>
                                            </div>
                                        </div>
                                        <!-- // 현금영수증 -->

                                        <!-- 결제수단 -->
                                        <script>
                                            $(document).ready(function () {
                                                // 결제수단
                                                $(".payment_method input").click(function () {
                                                    var $this = $(this)
                                                        , idx = $this.index(".payment_method input");
                                                    $(".pay_tab_wrap").addClass("hide").eq(idx).removeClass("hide");
                                                    if (idx == 2) {
                                                        $(".ord_info .ord_info_box table tr.without_handbook").addClass("on");
                                                    } else {
                                                        $(".ord_info .ord_info_box table tr.without_handbook").removeClass("on");
                                                    }

                                                    if (idx == 1 || idx == 2 || idx == 4) {
                                                        $(".cash_receipt_box").removeClass("hide");
                                                    } else {
                                                        $(".cash_receipt_box").addClass("hide");
                                                    }

                                                });

                                                $("input.rec_type").click(function () {
                                                    var $this = $(this),
                                                        idx = $this.index("input.rec_type");
                                                    $(".receipt_box.receipt_2").add("hide");
                                                    if (idx == 0) {
                                                        $(".receipt_type1").removeClass("hide").change();
                                                        $(".receipt_type2").addClass("hide");
                                                        $("select.receipt_type1, select.receipt_type2, .receipt_box input, .receipt_box select").prop("disabled", false);
                                                    } else if (idx == 1) {
                                                        $(".receipt_type1").addClass("hide");
                                                        $(".receipt_type2").removeClass("hide").change();
                                                        $("select.receipt_type1, select.receipt_type2, .receipt_box input, .receipt_box select").prop("disabled", false);
                                                    } else {
                                                        $("select.receipt_type1, select.receipt_type2, .receipt_box input, .receipt_box select").prop("disabled", true);
                                                    }
                                                });

                                                $(".receipt_type1").change(function () {
                                                    var $this = $(this), val = $this.val();
                                                    $(".receipt_box").addClass("hide");
                                                    $(".receipt_type1").removeClass("hide");
                                                    $(".receipt_type2").addClass("hide");

                                                    if (val == "") {
                                                        $(".receipt_box.receipt_1").removeClass("hide");
                                                        $(".receipt_box.receipt_2").addClass("hide");
                                                    } else {
                                                        $(".receipt_box.receipt_1").addClass("hide");
                                                        $(".receipt_box.receipt_2").removeClass("hide");
                                                    }
                                                });

                                                $(".receipt_type2").change(function () {
                                                    var $this = $(this), val = $this.val();
                                                    $(".receipt_box").addClass("hide");
                                                    $(".receipt_type1").addClass("hide");
                                                    $(".receipt_type2").removeClass("hide");

                                                    if (val == "") {
                                                        $(".receipt_box.receipt_3").removeClass("hide");
                                                        $(".receipt_box.receipt_4").addClass("hide");
                                                    } else {
                                                        $(".receipt_box.receipt_3").addClass("hide");
                                                        $(".receipt_box.receipt_4").removeClass("hide");
                                                    }
                                                });
                                            });
                                        </script>
                                        <!-- // 결제수단 -->

                                        <!-- 신용카드 결제 안내 -->
                                        <div class="pay_tab_wrap pay_tab_wrap_1 tab_wrap">
                                            <div class="pay_tab_nav tab_with_contents">
                                                <ul>
                                                    <li class="on"><a href="javascript:void(0);"><span>무이자 할부안내</span></a></li>
                                                    <li><a href="javascript:void(0);"><span>신용카드 이용안내</span></a></li>
                                                </ul>
                                            </div>

                                            <!-- 무이자 할부안내 -->
                                            <div class="tab_contents tab_contents1">
                                                <img src="/image/sample/img_card_event.gif" alt="">
                                            </div>
                                            <!-- // 무이자 할부안내 -->

                                            <!-- 신용카드 이용안내 -->
                                            <div class="tab_contents tab_contents2 hide">
                                                <p class="tit">신용카드 결제 안내</p>
                                                <ul class="dot_list">
                                                    <li>대상카드:BC/국민/외환/삼성/현대/신한(구LG)/수협/광주/우리(평화)씨티/하나(보람)/전북/농협NH/산은/제주
                                                    <li>카드 첫 결제 시 국민, BC, 우리카드의 경우 ISP 인증서를 다운받아야 사용할 수 있습니다.<br>삼성, 외환, 신한, 현대, 롯데, 하나 등의 카드는 각 카드사별
                                                        안심선택 회원에 가입하여야만 이용 가능합니다.
                                                    </li>
                                                    <li>삼성, 현대, 신한(구LG), 롯데 : 금액에 상관없이 안심선택 또는 공인인증서 선택</li>
                                                    <li>외환 - 금액에 상관없이 안심선택 적용.</li>
                                                </ul>
                                            </div>
                                            <!-- // 신용카드 이용안내 -->
                                        </div>
                                        <!-- // 신용카드 결제 안내 -->

                                        <!-- 실시간 계좌이체 -->
                                        <div class="pay_tab_wrap pay_tab_wrap_2  hide">
                                            <div class="pay_tab_nav">
                                                <ul>
                                                    <li class="on"><a href="javascript:void(0);"><span>실시간 계좌이체 안내</span></a></li>
                                                </ul>
                                            </div>

                                            <!-- 회원혜택 내용 -->
                                            <div class="tab_contents">
                                                <p class="tit">실시간 계좌이체 안내</p>
                                                <ul class="dot_list">
                                                    <li>실시간 계좌이체 서비스는 결제와 동시에 지정하신 계좌에서 바로 이체 처리 됩니다.</li>
                                                    <li>5만원 이상 거래시, 핸드폰인증 또는 공인인증서 중 선택하여 결제하셔야 합니다.</li>
                                                    <li>홈뱅킹, PC뱅킹, 인터넷뱅킹, 이용자는 지금 바로 이용할 수 있습니다.</li>
                                                    <li>신용카드가 없는 학생이나 미성년자도 이용할 수 있습니다.</li>
                                                    <li>등록하신 통장잔액이 결제일에 남아있지 않을 경우 이용료가 출금되지 않습니다.</li>
                                                    <li>현금 영수증은 결제 시 입력하신 주민등록번호 또는 휴대폰 번호로 발급 됩니다.</li>
                                                </ul>
                                            </div>
                                            <!-- // 회원혜택 내용 -->
                                        </div>
                                        <!-- // 신용카드 결제 안내 -->

                                        <!-- 무통장 입금 안내 -->
                                        <div class="pay_tab_wrap pay_tab_wrap_3  hide">
                                            <div class="pay_tab_nav">
                                                <ul>
                                                    <li class="on"><a href="javascript:void(0);"><span>무통장 입금 안내</span></a></li>
                                                </ul>
                                            </div>

                                            <!-- 회원혜택 내용 -->
                                            <div class="tab_contents">
                                                <p class="tit">무통장 입금 안내</p>
                                                <ul class="dot_list">
                                                    <li>입금 시 입급자 명을 정확하게 기재하여 주시기 바랍니다.</li>
                                                    <li>작성해주신 입금자명과 입금계좌의 입금자 명이 다른 경우 입금 확인이 늦어질 수 있습니다.</li>
                                                    <li>작성하신 입금자 명과 입금 계좌의 입금자 명이 다른 경우 고객센터(02-308-8880)으로 연락주시기 바랍니다.</li>
                                                    <li>무통장 입금 확인은 2~3일 정도 소요됩니다.</li>
                                                </ul>
                                            </div>
                                            <!-- // 회원혜택 내용 -->
                                        </div>
                                        <!-- // 무통장 입금 안내 -->

                                    </div>
                                </div>

                                <div class="ord_tot_info">
                                    <div class="ord_tot_info_box">
                                        <p class="tit">최종 결제 정보</p>
                                        <div class="ord_tot_list">
                                            <ul>
                                                <li>
                                                    <span class="label">총 상품금액</span>
                                                    <span class="val">
                                                            <span class="mont">129,000</span>
                                                            <span class="unit">원</span>
                                                        </span>
                                                </li>
                                                <li>
                                                    <span class="label">배송비</span>
                                                    <span class="val">
                                                            <span class="mont">+0</span>
                                                            <span class="unit">원</span>
                                                        </span>
                                                </li>
                                                <li>
                                                    <span class="label">할인쿠폰</span>
                                                    <span class="val">
                                                            <span class="mont">-0</span>
                                                            <span class="unit">원</span>
                                                        </span>
                                                </li>
                                                <li>
                                                    <span class="label">복지포인트</span>
                                                    <span class="val">
                                                            <span class="mont">-0P</span>
                                                            <span class="unit">원</span>
                                                        </span>
                                                </li>
                                                <li>
                                                    <span class="label">Y포인트</span>
                                                    <span class="val">
                                                            <span class="mont">-0P</span>
                                                            <span class="unit">원</span>
                                                        </span>
                                                </li>
                                            </ul>
                                            <ul class="total">
                                                <li>
                                                    <span class="label">결제수단</span>
                                                    <span class="val">신용카드</span>
                                                </li>
                                                <li>
                                                    <span class="label">최종 결제금액</span>
                                                    <span class="val">
                                                            <span class="mont">9,999,999</span>
                                                            <span class="unit">원</span>
                                                        </span>
                                                </li>
                                            </ul>
                                        </div>
                                        <div class="btn_wrap">
                                            <a href="/order/orderTest" class="btn btn_payment"><span>결제하기</span></a>
                                        </div>

                                        <div class="pay_agree_box">
                                            <div class="pay_agree">
                                                <input type="checkbox" id="chk_agree">
                                                <label for="chk_agree">구매 및 결제 진행 동의합니다. (필수)</label>
                                            </div>
                                            <div class="pay_agree_inbox">
                                                <p>고객님께서는 아래 내용에 대하여 동의를 거부하실 수 있으며, 거부 시 상품배송, 구매 및 결제, 일부 Y포인트 적립이 제한됩니다.고객님께서는 아래 내용에 대하여 동의를 거부하실
                                                    수 있으며, 거부 시 상품배송, 구매 및 결제, 일부 Y포인트 적립이 제한됩니다.고객님께서는 아래 내용에 대하여 동의를 거부하실 수 있으며, 거부 시 상품배송, 구매 및 결제,
                                                    일부 Y포인트 적립이 제한됩니다.고객님께서는 아래 내용에 대하여 동의를 거부하실 수 있으며, 거부 시 상품배송, 구매 및 결제, 일부 Y포인트 적립이 제한됩니다.</p>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <!-- // order_footer -->

                        </div>
                        <!-- // cart_box -->

                    </div>
                </div>
                <!-- // cart_wrap -->

                <!-- 모달 주소록 -->
                <div class="modal addr_list">
                    <div class="modal_layer">
                        <div class="modal_header">
                            <p class="tit">주소록</p>
                        </div>
                        <div class="modal_body">
                            <p class="tit">배송을 원하는 주소를 선택하시면 주문서에 입력됩니다.<br>새로운 배송지 추가는 주문서에서 신규 배송지 선택하여 추가해주시기 바랍니다.</p>
                            <table class="tbl">
                                <colgroup>
                                    <col style="width:70px;">
                                    <col style="width:70px;">
                                    <col style="width:auto;">
                                    <col style="width:115px;">
                                    <col style="width:70px;">
                                </colgroup>
                                <thead>
                                <tr>
                                    <th scope="col">배송지</th>
                                    <th scope="col">수령인</th>
                                    <th scope="col">주소</th>
                                    <th scope="col">연락처</th>
                                    <th scope="col"></th>
                                </tr>
                                </thead>
                                <tbody>
                                <tr>
                                    <td scope="row">[기본] 집</td>
                                    <td>홍길동</td>
                                    <td class="left">
                                        <p class="post_code">12345</p>
                                        <p class="addr_txt">서울특별시 강남구 테헤란로 14길 16, 5층</p>
                                    </td>
                                    <td><p class="phone_no">010-12345678</p></td>
                                    <td class="btn_td">
                                        <a href="javascript:void(0);" class="btn m_w sml dark"><span>선택</span></a>
                                    </td>
                                </tr>
                                <tr>
                                    <td scope="row">회사</td>
                                    <td>홍길동</td>
                                    <td class="left">
                                        <p class="post_code">12345</p>
                                        <p class="addr_txt">서울특별시 강남구 테헤란로 14길 16, 5층</p>
                                    </td>
                                    <td><p class="phone_no">010-12345678</p></td>
                                    <td class="btn_td">
                                        <a href="javascript:void(0);" class="btn m_w sml dark"><span>선택</span></a>
                                        <a href="javascript:void(0);" class="btn m_w sml brd"><span>삭제</span></a>
                                    </td>
                                </tr>
                                </tbody>
                            </table>

                            <div class="btn_wrap">
                                <a href="javascript:void(0);" class="btn m_w sml cancel close"><span>취소</span></a>
                            </div>
                        </div>
                        <div class="modal_close">
                            <a href="javascript:void(0);"><span>닫기</span></a>
                        </div>
                    </div>
                </div>
                <!-- 모달 주소록 -->

                <!-- 모달 쿠폰적용 -->
                <div class="modal coupon_list">
                    <div class="modal_layer">
                        <div class="modal_header">
                            <p class="tit">쿠폰 적용</p>
                        </div>
                        <div class="modal_body">

                            <div class="coupon_max_apply">
                                <a href="javascript:void(0);" class="btn sml blue"><span>쿠폰 최대할인 적용</span></a>
                            </div>

                            <table class="tbl">
                                <colgroup>
                                    <col style="width:auto;">
                                    <col style="width:105px;">
                                    <col style="width:91px;">
                                    <col style="width:192px;">

                                </colgroup>
                                <thead>
                                <tr>
                                    <th scope="col">상품정보</th>
                                    <th scope="col">상품금액</th>
                                    <th scope="col">할인금액</th>
                                    <th scope="col">쿠폰선택</th>
                                </tr>
                                </thead>
                                <tbody>
                                <tr>
                                    <td scope="row" class="left">
                                        <div class="goods_info">
                                            <p class="nm">LG전자★LG전자&amp;교육할인공동기획★[LG전자] 초경량그램14 14ZD9</p>
                                            <p class="opt">옵션1: 화이트/옵션2 : 500G</p>
                                        </div>
                                    </td>
                                    <td>
                                                <span class="goods_price">
                                                    <span class="mont">154,000</span>
                                                    <span class="unit">원</span>
                                                </span>
                                    </td>
                                    <td>
                                                <span class="dc_price">
                                                    <span class="mont">1,700</span>
                                                    <span class="unit">원</span>
                                                </span>
                                    </td>
                                    <td class="left">
                                        <select class="sel">
                                            <option>쿠폰선택</option>
                                        </select>
                                    </td>
                                </tr>
                                <tr>
                                    <td scope="row" class="left">
                                        <div class="goods_info">
                                            <p class="nm">LG전자★LG전자&amp;교육할인공동기획★[LG전자] 초경량그램14 14ZD9</p>
                                            <p class="opt">옵션1: 화이트/옵션2 : 500G</p>
                                        </div>
                                    </td>
                                    <td>
                                                <span class="goods_price">
                                                    <span class="mont">154,000</span>
                                                    <span class="unit">원</span>
                                                </span>
                                    </td>
                                    <td>
                                                <span class="dc_price">
                                                    <span class="mont">0</span>
                                                    <span class="unit">원</span>
                                                </span>
                                    </td>
                                    <td class="left">
                                        적용 가능한 쿠폰이 없습니다.
                                    </td>
                                </tr>
                                </tbody>
                            </table>

                            <div class="coupon_dc_price">
                                <span>총 할인금액</span>
                                <span class="dc_price">
                                        <span class="mont">1,700</span>
                                        <span class="unit">원</span>
                                    </span>
                            </div>

                            <div class="btn_wrap">
                                <a href="javascript:void(0);" class="btn sml cancel m_w close"><span>취소</span></a>
                                <a href="javascript:void(0);" class="btn sml dark m_w"><span>적용</span></a>
                            </div>

                        </div>
                        <div class="modal_close">
                            <a href="javascript:void(0);"><span>닫기</span></a>
                        </div>
                    </div>
                </div>
                <!-- 모달 쿠폰적용 -->

            </section>
        </main>
        <!-- // content_wrap -->

        <!-- footer -->
        <footer id="footer">
            <div class="footer_top">
                <div class="in_container">
                    <div class="footer_top_box">
                        <ul>
                            <li><a href="javascript:void(0);"><span>교육할인 스토어란?</span></a></li>
                            <li><a href="javascript:void(0);"><span>이용약관</span></a></li>
                            <li><a href="javascript:void(0);" class="privace_rule"><span>개인정보취급방침</span></a></li>
                            <li><a href="javascript:void(0);"><span>입점 및 제휴문의</span></a></li>
                        </ul>
                    </div>

                    <div class="footer_top_box">
                        <ul>
                            <li><a href="javascript:void(0);"><span>관련사이트 바로가기</span></a></li>
                        </ul>
                    </div>
                </div>
            </div>

            <div class="in_container">
                <div class="footer_btm">
                    <div class="footer_btm_box logo">
                        <img src="/image/sample/img_footer_ygoon_logo.gif" alt="YGOON 교육할인스토어">
                    </div>
                    <div class="footer_btm_box copyright">
                        <ul>
                            <li>업체명 (주)와이티엔</li>
                            <li>대표이사 : 조준희</li>
                            <li>주소 : 서울특별시 마포구 상암산로 76 (상암동 1607, YTN 뉴스퀘어)</li>
                            <li>사업자 등록번호 : 102-81-32883</li>
                            <li>통신판매업 : 마포 1507 <a href="javascript:void(0);" target="_blank">[사업자번호 확인하기]</a></li>
                            <li>전화 : 02-398-8880 팩스 : 02-398-8359</li>
                            <li>이메일 : csmaster@ytn.co.kr</li>
                            <li>개인정보책임관리자 : 이교준</li>
                            <li>copyright (c) 2017 (주)와이티엔. all rights reserved.</li>
                        </ul>
                    </div>
                    <div class="footer_btm_box cs_info">
                        <p class="cs_info_tit">고객센터</p>
                        <div class="cs_info_phone">
                            <span class="mont">02-398-8880</span>
                            <span class="mont">csmaster@ytn.co.kr</span>
                        </div>
                        <ul>
                            <li>평일 09:00~18:00</li>
                            <li>점심시간 11:30~13:00 (주말 및 법정공휴일 휴무)</li>
                            <li>주말 및 공휴일은 1:1문의하기를 이용해주세요</li>
                        </ul>
                        <div class="btn_wrap">
                            <a href="javascript:void(0);" class="btn"><span>자주하는 질문</span></a>
                            <a href="javascript:void(0);" class="btn"><span>1:1 문의하기</span></a>
                        </div>
                    </div>
                </div>
            </div>
        </footer>
        <!-- // footer -->

        <!-- wing -->
        <div class="wing">
            <div class="recent_view">
                <div class="recent_view_title">
                    <strong>HISTORY</strong>
                    <span>3</span>
                </div>
                <div class="recent_view_list">
                    <p class="recent_view_list_nodata hide">최근본상품이 없습니다.</p>
                    <ul>
                        <li>
                            <a href="#;">
                                <span class="thumb"><img src="http://via.placeholder.com/90x90/?text=1" alt=""></span>
                                <span class="frame"></span>
                            </a>
                            <a href="#;" class="recent_view_del"><span>삭제</span></a>
                            <div class="recent_view_info">
                                <p>[레드딜] 구찌&헤밀턴 강렬한 11월 폭탄할인!</p>
                                <span class="price"><span class="mont">436,000</span><span class="won">원</span></span>
                            </div>
                        </li>
                        <li>
                            <a href="#;">
                                <span class="thumb"><img src="http://via.placeholder.com/90x90/?text=2" alt=""></span>
                                <span class="frame"></span>
                            </a>
                            <a href="#;" class="recent_view_del"><span>삭제</span></a>
                            <div class="recent_view_info">
                                <p>[레드딜] 구찌&헤밀턴 강렬한 11월 폭탄할인!</p>
                                <span class="price"><span class="mont">436,000</span><span class="won">원</span></span>
                            </div>
                        </li>
                        <li>
                            <a href="#;">
                                <span class="thumb"><img src="http://via.placeholder.com/90x90/?text=3" alt=""></span>
                                <span class="frame"></span>
                            </a>
                            <a href="#;" class="recent_view_del"><span>삭제</span></a>
                            <div class="recent_view_info">
                                <p>[레드딜] 구찌&헤밀턴 강렬한 11월 폭탄할인!</p>
                                <span class="price"><span class="mont">436,000</span><span class="won">원</span></span>
                            </div>
                        </li>
                        <li>
                            <a href="#;">
                                <span class="thumb"><img src="http://via.placeholder.com/90x90/?text=4" alt=""></span>
                                <span class="frame"></span>
                            </a>
                            <a href="#;" class="recent_view_del"><span>삭제</span></a>
                            <div class="recent_view_info">
                                <p>[레드딜] 구찌&헤밀턴 강렬한 11월 폭탄할인!</p>
                                <span class="price"><span class="mont">436,000</span><span class="won">원</span></span>
                            </div>
                        </li>
                        <li>
                            <a href="#;">
                                <span class="thumb"><img src="http://via.placeholder.com/90x90/?text=5" alt=""></span>
                                <span class="frame"></span>
                            </a>
                            <a href="#;" class="recent_view_del"><span>삭제</span></a>
                            <div class="recent_view_info">
                                <p>[레드딜] 구찌&헤밀턴 강렬한 11월 폭탄할인!</p>
                                <span class="price"><span class="mont">436,000</span><span class="won">원</span></span>
                            </div>
                        </li>
                        <li>
                            <a href="#;">
                                <span class="thumb"><img src="http://via.placeholder.com/90x90/?text=6" alt=""></span>
                                <span class="frame"></span>
                            </a>
                            <a href="#;" class="recent_view_del"><span>삭제</span></a>
                            <div class="recent_view_info">
                                <p>[레드딜] 구찌&헤밀턴 강렬한 11월 폭탄할인!</p>
                                <span class="price"><span class="mont">436,000</span><span class="won">원</span></span>
                            </div>
                        </li>
                        <li>
                            <a href="#;">
                                <span class="thumb"><img src="http://via.placeholder.com/90x90/?text=7" alt=""></span>
                                <span class="frame"></span>
                            </a>
                            <a href="#;" class="recent_view_del"><span>삭제</span></a>
                            <div class="recent_view_info">
                                <p>[레드딜] 구찌&헤밀턴 강렬한 11월 폭탄할인!</p>
                                <span class="price"><span class="mont">436,000</span><span class="won">원</span></span>
                            </div>
                        </li>
                    </ul>
                </div>
                <div class="recent_view_foot">
                    <a href="#;" class="recent_view_prev"><span>이전</span></a>
                    <span class="recent_view_info">
                            <strong>1</strong><span>/1</span>
                        </span>
                    <a href="#;" class="recent_view_next"><span>다음</span></a>
                </div>
            </div>

            <div class="promotion_banner">
                <a href="javascript:void(0);"><img src="http://via.placeholder.com/100x100/?text=BANNER"></a>
            </div>

            <div class="go_top">
                <a href="#top"><img src="/image/common/btn_go_top.gif" alt="위로가기"></a>
            </div>
        </div>
        <!-- // wing -->

    </div>
    <!-- // container -->
</div>
<!-- // wrap -->
<script>
    $(document).ready(function () {
        $.ajax({
            method: "POST",
            url: "/api/tempOrder/read",
            data: {id: 50}
        }).done(function (data) {
            alert("Data read: " + data);
        });
    })

    function getTemporderData() {
        $.ajax({
            method: "GET",
            url: "/order/orderTest",
        }).done(function (data) {
            alert("Data read: " + data);
        });

    }
</script>
</body>
</html>
