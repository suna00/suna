<%@ page language="java" contentType="text/html;charset=UTF-8"%>
<%
    /* ============================================================================== */
    /* =   PAGE : 결제 요청 PAGE                                                    = */
    /* = -------------------------------------------------------------------------- = */
    /* =   이 페이지는 Payplus Plug-in을 통해서 결제자가 결제 요청을 하는 페이지    = */
    /* =   입니다. 아래의 ※ 필수, ※ 옵션 부분과 매뉴얼을 참조하셔서 연동을        = */
    /* =   진행하여 주시기 바랍니다.                                                = */
    /* = -------------------------------------------------------------------------- = */
    /* =   연동시 오류가 발생하는 경우 아래의 주소로 접속하셔서 확인하시기 바랍니다.= */
    /* =   접속 주소 : http://kcp.co.kr/technique.requestcode.do                    = */
    /* = -------------------------------------------------------------------------- = */
    /* =   Copyright (c)  2016  NHN KCP Inc.   All Rights Reserverd.                = */
    /* ============================================================================== */
%>
<%
    /* ============================================================================== */
    /* =   환경 설정 파일 Include                                                   = */
    /* = -------------------------------------------------------------------------- = */
    /* =   ※ 필수                                                                  = */
    /* =   테스트 및 실결제 연동시 site_conf_inc.jsp 파일을 수정하시기 바랍니다.    = */
    /* = -------------------------------------------------------------------------- = */
%>
    <%@ include file="../order/cfg/site_conf_inc.jsp"%>
<%!
    /* = -------------------------------------------------------------------------- = */
    /* =   환경 설정 파일 Include END                                               = */
    /* ============================================================================== */
%>
<%!
    /* ============================================================================== */
    /* =   null 값을 처리하는 메소드                                                = */
    /* = -------------------------------------------------------------------------- = */
      public String f_get_parm( String val )
        {
        if ( val == null ) val = "";
            return  val;
        }
    /* ============================================================================== */
%>
<%
  request.setCharacterEncoding( "utf-8" );

    /* kcp와 통신후 kcp 서버에서 전송되는 결제 요청 정보 */
    String req_tx          = f_get_parm( request.getParameter( "req_tx"         ) ); // 요청 종류         
    String res_cd          = f_get_parm( request.getParameter( "res_cd"         ) ); // 응답 코드         
    String tran_cd         = f_get_parm( request.getParameter( "tran_cd"        ) ); // 트랜잭션 코드     
    String ordr_idxx       = f_get_parm( request.getParameter( "ordr_idxx"      ) ); // 쇼핑몰 주문번호   
    String good_name       = f_get_parm( request.getParameter( "good_name"      ) ); // 상품명            
    String good_mny        = f_get_parm( request.getParameter( "good_mny"       ) ); // 결제 총금액       
    String buyr_name       = f_get_parm( request.getParameter( "buyr_name"      ) ); // 주문자명          
    String buyr_tel1       = f_get_parm( request.getParameter( "buyr_tel1"      ) ); // 주문자 전화번호   
    String buyr_tel2       = f_get_parm( request.getParameter( "buyr_tel2"      ) ); // 주문자 핸드폰 번호
    String buyr_mail       = f_get_parm( request.getParameter( "buyr_mail"      ) ); // 주문자 E-mail 주소
    String use_pay_method  = f_get_parm( request.getParameter( "use_pay_method" ) ); // 결제 방법          
	String enc_info        = f_get_parm( request.getParameter( "enc_info"       ) ); // 암호화 정보       
    String enc_data        = f_get_parm( request.getParameter( "enc_data"       ) ); // 암호화 데이터     
    String cash_yn         = f_get_parm( request.getParameter( "cash_yn"        ) );
    String cash_tr_code    = f_get_parm( request.getParameter( "cash_tr_code"   ) );
    /* 기타 파라메터 추가 부분 - Start - */
    String param_opt_1    = f_get_parm( request.getParameter( "param_opt_1"     ) ); // 기타 파라메터 추가 부분
    String param_opt_2    = f_get_parm( request.getParameter( "param_opt_2"     ) ); // 기타 파라메터 추가 부분
    String param_opt_3    = f_get_parm( request.getParameter( "param_opt_3"     ) ); // 기타 파라메터 추가 부분
    /* 기타 파라메터 추가 부분 - End -   */

  String tablet_size     = "1.0"; // 화면 사이즈 고정
  String url             = request.getRequestURL().toString();
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="ko" xml:lang="ko">
<head>
<title>*** NHN KCP [AX-HUB Version] ***</title>
<meta http-equiv="Content-Type" content="text/html; charset=euc-kr">
<meta http-equiv="Cache-Control" content="No-Cache">
<meta http-equiv="Pragma" content="No-Cache">

<meta name="viewport" content="width=device-width, user-scalable=<%=tablet_size%>, initial-scale=<%=tablet_size%>, maximum-scale=<%=tablet_size%>, minimum-scale=<%=tablet_size%>">

<link href="/order/js/style.css" rel="stylesheet" type="text/css" id="cssLink"/>

<!-- 거래등록 하는 kcp 서버와 통신을 위한 스크립트-->
<script>

    var ajax_flag = true;
    var req01_AJAX;
    var PayUrl="";

    function initRequest()
    {
        if ( window.XMLHttpRequest )
        {
            return new XMLHttpRequest();
        } else if ( window.ActiveXObject )
        {
            return new ActiveXObject( "Microsoft.XMLHTTP" );
        }
    }

    function sendRequest( url )
    {
        req01_AJAX = null;
        req01_AJAX = initRequest();

        if ( req01_AJAX )
        {
            req01_AJAX.onreadystatechange = process_AJAX;
            req01_AJAX.open( "POST", url, true );
            req01_AJAX.send( null );
        }
    }

    function kcp_AJAX()
    {
        // 해당 소스는 jQuery를 사용 하지 않는 업체에서도 작동 할수 있도록 구현되었습니다.
        // 이 부분은 javascript로 구현시 jQuery를 이용하면 더욱 쉽게 구현 할수 있습니다.
        // 또는
        // javascript를 통한 ajax 통신을 할 필요 없이 소스내에 order_approval.jsp를 연동해서 사용 하셔도 관계 없으며
        // 제공되는 jar 파일에 직접 전달된 데이터를 사용할수 있도록 method가 구현 되어 있습니다.
        if(ajax_flag)
        {
            var url    = "order_approval";
            var form = document.order_info;
            var params = "?site_cd=" + form.site_cd.value
                + "&ordr_idxx=" + form.ordr_idxx.value
                + "&good_mny=" + form.good_mny.value
                + "&pay_method=" + form.pay_method.value
                + "&escw_used=" + form.escw_used.value
                + "&good_name=" + form.good_name.value
                + "&response_type=" + form.response_type.value
                + "&Ret_URL=" + form.Ret_URL.value;
            sendRequest( url + params );

            ajax_flag = false;
        }
        else
        {
            alert("통신 중입니다. 잠시만 기다려 주세요.");
        }

    }

    function process_AJAX()
    {
        if ( req01_AJAX.readyState == 4 ) //READY_STATE_COMPLETE = 4
        {
            if ( req01_AJAX.status == 200 )
            {
                if ( req01_AJAX.responseText != null )
                {
                    // 해당 소스는 jQuery를 사용 하지 않는 업체에서도 작동 할수 있도록 구현되었습니다.
                    // 이 부분은 javascript로 구현시 jQuery를 이용하면 더욱 쉽게 구현 할수 있습니다.
                    // 또는
                    // javascript를 통한 ajax 통신을 할 필요 없이 소스내에 order_approval.jsp를 연동해서 사용 하셔도 관계 없으며
                    // 제공되는 jar 파일에 직접 전달된 데이터를 사용할수 있도록 method가 구현 되어 있습니다.
                    try
                    {
                        var form = document.order_info;

                        if( form.response_type.value == "JSON" )
                        {
                            StateChangeForJSON( req01_AJAX ); // JSON 방식일 경우
                        }
                        else if( form.response_type.value == "XML" )
                        {
                            StateChangeForXML( req01_AJAX ); // XML 방식일 경우
                        }
                        else if( form.response_type.value == "TEXT" || form.response_type.value == "" )
                        {
                            StateChangeForText( req01_AJAX ); // TEXT 방식일 경우
                        }
                    }
                    catch( e )
                    {
                        StateChangeForText( req01_AJAX ); // TEXT 방식일 경우
                    }
                }
            }
            else
            {
                ajax_flag=true;
                alert( req01_AJAX.responseJSON );
            }
        }
    }

    function StateChangeForText( req )
    {
        var txt = req01_AJAX.responseText.split( ":" );

        if( txt[0].replace(/^\s*/,'').replace(/\s*$/,'') == '0000' )
        {
            document.getElementById( "approval" ).value = txt[1].replace(/^\s*/,'').replace(/\s*$/,'');
            // 아래 alert는 삭제 해도됨
            alert("성공적으로 거래가 등록 되었습니다.");
            PayUrl = decodeURIComponent( txt[2].replace(/^\s*/,'').replace(/\s*$/,'') );

            document.getElementById( "traceNo" ).value = txt[3].replace(/^\s*/,'').replace(/\s*$/,'');

            if( txt.length == 6 )
                document.getElementById( "PayUrl"  ).value = txt[5].replace(/^\s*/,'').replace(/\s*$/,'');

            call_pay_form();
        }
        else
        {
            var res_msg = txt[2].replace(/^\s*/,'').replace(/\s*$/,'');

            ajax_flag=true;
            alert("실패 되었습니다.[" + res_msg + "]");
        }
    }

    function StateChangeForXML( req )
    {
        var xmlDoc = ( new DOMParser() ).parseFromString( req.responseText.replace(/^\s*/,'').replace(/\s*$/,''), "text/xml" ).documentElement;

        var x = xmlDoc.getElementsByTagName( "data" );

        if( x[0].getElementsByTagName( "Code" )[0].childNodes[0].nodeValue == '0000' )
        {
            document.getElementById( "approval" ).value = x[0].getElementsByTagName( "approvalKey" )[0].childNodes[0].nodeValue;
            // 아래 alert는 삭제 해도됨
            alert("성공적으로 거래가 등록 되었습니다.");
            PayUrl = x[0].getElementsByTagName( "PayUrl" )[0].childNodes[0].nodeValue;

            document.getElementById( "traceNo" ).value = x[0].getElementsByTagName( "traceNo"     )[0].childNodes[0].nodeValue;

            try{
                document.getElementById( "PayUrl"  ).value = x[0].getElementsByTagName( "request_URI" )[0].childNodes[0].nodeValue;
            }catch(e){}

            call_pay_form();
        }
        else
        {
            ajax_flag=true;

            alert("실패 되었습니다.[" + x[0].getElementsByTagName( "Message" )[0].childNodes[0].nodeValue + "]");
        }
    }


    function StateChangeForJSON( xmlHttpRequest )
    {
        var json = eval('('+ xmlHttpRequest.responseText +')');

        if( json.Code == '0000' )
        {
            document.getElementById( "approval" ).value = json.approvalKey;
            // 아래 alert는 삭제 해도됨
            alert("성공적으로 거래가 등록 되었습니다.");
            PayUrl = json.PayUrl;

            document.getElementById( "PayUrl"  ).value = json.request_URI;
            document.getElementById( "traceNo" ).value = json.traceNo;

            call_pay_form();
        }
        else
        {
            ajax_flag=true;

            alert("실패 되었습니다.[" + json.Message + "]");
        }
    }

</script>

<script type="text/javascript">
  var controlCss = "css/style_mobile.css";
  var isMobile = {
    Android: function() {
      return navigator.userAgent.match(/Android/i);
    },
    BlackBerry: function() {
      return navigator.userAgent.match(/BlackBerry/i);
    },
    iOS: function() {
      return navigator.userAgent.match(/iPhone|iPad|iPod/i);
    },
    Opera: function() {
      return navigator.userAgent.match(/Opera Mini/i);
    },
    Windows: function() {
      return navigator.userAgent.match(/IEMobile/i);
    },
    any: function() {
      return (isMobile.Android() || isMobile.BlackBerry() || isMobile.iOS() || isMobile.Opera() || isMobile.Windows());
    }
  };

  if( isMobile.any() )
    document.getElementById("cssLink").setAttribute("href", controlCss);
</script>
<script type="text/javascript">
   /* 주문번호 생성 예제 */
  function init_orderid()
  {
    var today = new Date();
    var year  = today.getFullYear();
    var month = today.getMonth() + 1;
    var date  = today.getDate();
    var time  = today.getTime();

    if (parseInt(month) < 10)
      month = "0" + month;

    if (parseInt(date) < 10)
      date  = "0" + date;

    var order_idxx = "TEST" + year + "" + month + "" + date + "" + time;
    var ipgm_date  = year + "" + month + "" + date;

    document.order_info.ordr_idxx.value = order_idxx;
    document.order_info.ipgm_date.value = ipgm_date;
  }

   /* kcp web 결제창 호츨 (변경불가) */
  function call_pay_form()
  {
    var v_frm = document.order_info;
	
    v_frm.action = PayUrl;

    if (v_frm.Ret_URL.value == "")
    {
	  /* Ret_URL값은 현 페이지의 URL 입니다. */
	  alert("연동시 Ret_URL을 반드시 설정하셔야 됩니다.");
      return false;
    }
    else
    {
      v_frm.submit();
    }
  }

   /* kcp 통신을 통해 받은 암호화 정보 체크 후 결제 요청 (변경불가) */
  function chk_pay()
  {
    self.name = "tar_opener";
    var pay_form = document.pay_form;

    if (pay_form.res_cd.value == "3001" )
    {
      alert("사용자가 취소하였습니다.");
      pay_form.res_cd.value = "";
    }

    if (pay_form.enc_info.value)
      pay_form.submit();
  }

  function jsf__chk_type()
  {
    if ( document.order_info.ActionResult.value == "card" )
    {
      document.order_info.pay_method.value = "CARD";
    }
    else if ( document.order_info.ActionResult.value == "acnt" )
    {
      document.order_info.pay_method.value = "BANK";
    }
    else if ( document.order_info.ActionResult.value == "vcnt" )
    {
      document.order_info.pay_method.value = "VCNT";
    }
    else if ( document.order_info.ActionResult.value == "mobx" )
    {
      document.order_info.pay_method.value = "MOBX";
    }
    else if ( document.order_info.ActionResult.value == "ocb" )
    {
      document.order_info.pay_method.value = "TPNT";
      document.order_info.van_code.value = "SCSK";
    }
    else if ( document.order_info.ActionResult.value == "tpnt" )
    {
      document.order_info.pay_method.value = "TPNT";
      document.order_info.van_code.value = "SCWB";
    }
    else if ( document.order_info.ActionResult.value == "scbl" )
    {
      document.order_info.pay_method.value = "GIFT";
      document.order_info.van_code.value = "SCBL";
    }
    else if ( document.order_info.ActionResult.value == "sccl" )
    {
      document.order_info.pay_method.value = "GIFT";
      document.order_info.van_code.value = "SCCL";
    }
    else if ( document.order_info.ActionResult.value == "schm" )
    {
      document.order_info.pay_method.value = "GIFT";
      document.order_info.van_code.value = "SCHM";
    }
  }
</script>
</head>

<body onload="jsf__chk_type();init_orderid();chk_pay();">
<div id="sample_wrap">
<form name="order_info" method="post">
  <!-- 타이틀 -->
  <h1>[결제요청] <span>이 페이지는 결제를 요청하는 샘플(예시) 페이지입니다.</span></h1>

  <div class="sample">

    <!-- 상단 문구 -->
    <p>
      이 페이지는 결제를 요청하는 페이지입니다
    </p>

    <!-- 주문 정보 -->
    <h2>&sdot; 주문 정보</h2>
    <table class="tbl" cellpadding="0" cellspacing="0">
      <tr>
        <th>지불 방법</th>
        <td>
            <select name="ActionResult" onchange="jsf__chk_type();">
                <option value="" selected>선택하십시오</option>
                <option value="card">신용카드</option>
                <option value="acnt">계좌이체</option>
                <option value="vcnt">가상계좌</option>
                <option value="mobx">휴대폰</option>
                <option value="ocb">OK캐쉬백</option>
                <option value="tpnt">복지포인트</option>
                <option value="scbl">도서상품권</option>
                <option value="sccl">문화상품권</option>
                <option value="schm">해피머니</option>
            </select>
        </td>
      </tr>
      <tr>
        <th>주문 번호</th>
        <td><input type="text" name="ordr_idxx" class="w200" value=""></td>
      </tr>
      <tr>
        <th>상품명</th>
        <td><input type="text" name="good_name" class="w100" value="핸드폰"></td>
      </tr>
      <tr>
        <th>결제 금액</th>
        <td><input type="text" name="good_mny" class="w100" value="1004"></td>
      </tr>
      <tr>
        <th>주문자명</th>
        <td><input type="text" name="buyr_name" class="w100" value="홍길동"></td>
      </tr>
      <tr>
        <th>E-mail</th>
        <td><input type="text" name="buyr_mail" class="w200" value="test@test.co.kr"></td>
      </tr>
      <tr>
        <th>전화번호</th>
        <td><input type="text" name="buyr_tel1" class="w100" value="02-2108-1000"></td>
      </tr>
      <tr>
        <th>휴대폰번호</th>
        <td><input type="text" name="buyr_tel2" class="w100" value="010-0000-0000"></td>
      </tr>
    </table>

    <!-- 결제 요청/처음으로 이미지 -->
    <div class="footer">
        <b>※ PC에서 결제요청시 오류가 발생합니다. ※</b>
    </div>
    <div class="btnset" id="display_pay_button" style="display:block">
      <input name="" type="button" class="submit" value="결제요청" onclick="kcp_AJAX();">
      <a href="../index.html" class="home">처음으로</a>
    </div>
  </div>
  <!--footer-->
  <div class="footer">
    Copyright (c) NHN KCP INC. All Rights reserved.
  </div>
  <!--//footer-->

  <!-- 공통정보 -->
  <input type="hidden" name="req_tx"          value="pay">                           <!-- 요청 구분 -->
  <input type="hidden" name="shop_name"       value="<%= g_conf_site_name %>">       <!-- 사이트 이름 --> 
  <input type="hidden" name="site_cd"         value="<%= g_conf_site_cd   %>">       <!-- 사이트 키 -->
  <input type="hidden" name="currency"        value="410"/>                          <!-- 통화 코드 -->
  <!-- 결제등록 키 -->
  <input type="hidden" name="approval_key"    id="approval">
  <!-- 인증시 필요한 파라미터(변경불가)-->
  <input type="hidden" name="escw_used"       value="N">
  <input type="hidden" name="pay_method"      value="">
  <input type="hidden" name="van_code"        value="">
  <!-- 신용카드 설정 -->
  <input type="hidden" name="quotaopt"        value="12"/>                           <!-- 최대 할부개월수 -->
  <!-- 가상계좌 설정 -->
  <input type="hidden" name="ipgm_date"       value=""/>

  <!-- 리턴 URL (kcp와 통신후 결제를 요청할 수 있는 암호화 데이터를 전송 받을 가맹점의 주문페이지 URL) -->
  <input type="hidden" name="Ret_URL"         value="<%=url%>">
  <!-- 화면 크기조정 -->
  <input type="hidden" name="tablet_size"     value="<%=tablet_size%>">

  <!-- 추가 파라미터 ( 가맹점에서 별도의 값전달시 param_opt 를 사용하여 값 전달 ) -->
  <input type="hidden" name="param_opt_1"     value="">
  <input type="hidden" name="param_opt_2"     value="">
  <input type="hidden" name="param_opt_3"     value="">

  <!-- 결제 정보 등록시 응답 타입 ( 필드가 없거나 값이 '' 일경우 TEXT, 값이 XML 또는 JSON 지원 -->
  <input type="hidden" name="response_type"  value="TEXT"/>
  <input type="hidden" name="PayUrl"   id="PayUrl"   value=""/>
  <input type="hidden" name="traceNo"  id="traceNo"  value=""/>

<%
    /* ============================================================================== */
    /* =   옵션 정보                                                                = */
    /* = -------------------------------------------------------------------------- = */
    /* =   ※ 옵션 - 결제에 필요한 추가 옵션 정보를 입력 및 설정합니다.             = */
    /* = -------------------------------------------------------------------------- = */
    /* 카드사 리스트 설정
    예) 비씨카드와 신한카드 사용 설정시
    <input type="hidden" name="used_card"    value="CCBC:CCLG">

    /*  무이자 옵션
            ※ 설정할부    (가맹점 관리자 페이지에 설정 된 무이자 설정을 따른다)                             - "" 로 설정
            ※ 일반할부    (KCP 이벤트 이외에 설정 된 모든 무이자 설정을 무시한다)                           - "N" 로 설정
            ※ 무이자 할부 (가맹점 관리자 페이지에 설정 된 무이자 이벤트 중 원하는 무이자 설정을 세팅한다)   - "Y" 로 설정
    <input type="hidden" name="kcp_noint"       value=""/> */

    /*  무이자 설정
            ※ 주의 1 : 할부는 결제금액이 50,000 원 이상일 경우에만 가능
            ※ 주의 2 : 무이자 설정값은 무이자 옵션이 Y일 경우에만 결제 창에 적용
            예) 전 카드 2,3,6개월 무이자(국민,비씨,엘지,삼성,신한,현대,롯데,외환) : ALL-02:03:04
            BC 2,3,6개월, 국민 3,6개월, 삼성 6,9개월 무이자 : CCBC-02:03:06,CCKM-03:06,CCSS-03:06:04
    <input type="hidden" name="kcp_noint_quota" value="CCBC-02:03:06,CCKM-03:06,CCSS-03:06:09"/> */

    /* KCP는 과세상품과 비과세상품을 동시에 판매하는 업체들의 결제관리에 대한 편의성을 제공해드리고자, 
       복합과세 전용 사이트코드를 지원해 드리며 총 금액에 대해 복합과세 처리가 가능하도록 제공하고 있습니다
       복합과세 전용 사이트 코드로 계약하신 가맹점에만 해당이 됩니다
       상품별이 아니라 금액으로 구분하여 요청하셔야 합니다
       총결제 금액은 과세금액 + 부과세 + 비과세금액의 합과 같아야 합니다. 
       (good_mny = comm_tax_mny + comm_vat_mny + comm_free_mny)

        <input type="hidden" name="tax_flag"       value="TG03">  <!-- 변경불가	   -->
        <input type="hidden" name="comm_tax_mny"   value=""    >  <!-- 과세금액	   --> 
        <input type="hidden" name="comm_vat_mny"   value=""    >  <!-- 부가세	   -->
        <input type="hidden" name="comm_free_mny"  value=""    >  <!-- 비과세 금액 --> */
		
	/* 결제창 한국어/영어 설정 옵션 (Y : 영어)
		<input type="hidden" name="eng_flag"        value="Y"/> */                     
		  
    /* 가맹점에서 관리하는 고객 아이디 설정을 해야 합니다. 상품권 결제 시 반드시 입력하시기 바랍니다.
        <input type="hidden" name="shop_user_id"    value=""/> */
  
    /* 복지포인트 결제시 가맹점에 할당되어진 코드 값을 입력해야합니다.
        <input type="hidden" name="pt_memcorp_cd"   value=""/> */
		
    /* 결제창 현금영수증 노출 설정 옵션 (Y : 노출)
        <input type="hidden" name="disp_tax_yn"     value="Y"/> */
    /* = -------------------------------------------------------------------------- = */
    /* =   옵션 정보 END                                                            = */
    /* ============================================================================== */
%>

</form>
</div>
<form name="pay_form" method="post" action="pp_cli_hub.jsp">
    <input type="hidden" name="req_tx"         value="<%=req_tx%>">               <!-- 요청 구분          -->
    <input type="hidden" name="res_cd"         value="<%=res_cd%>">               <!-- 결과 코드          -->
    <input type="hidden" name="tran_cd"        value="<%=tran_cd%>">              <!-- 트랜잭션 코드      -->
    <input type="hidden" name="ordr_idxx"      value="<%=ordr_idxx%>">            <!-- 주문번호           -->
    <input type="hidden" name="good_mny"       value="<%=good_mny%>">             <!-- 휴대폰 결제금액    -->
    <input type="hidden" name="good_name"      value="<%=good_name%>">            <!-- 상품명             -->
    <input type="hidden" name="buyr_name"      value="<%=buyr_name%>">            <!-- 주문자명           -->
    <input type="hidden" name="buyr_tel1"      value="<%=buyr_tel1%>">            <!-- 주문자 전화번호    -->
    <input type="hidden" name="buyr_tel2"      value="<%=buyr_tel2%>">            <!-- 주문자 휴대폰번호  -->
    <input type="hidden" name="buyr_mail"      value="<%=buyr_mail%>">            <!-- 주문자 E-mail      -->
    <input type="hidden" name="cash_yn"		   value="<%=cash_yn%>">              <!-- 현금영수증 등록여부-->
    <input type="hidden" name="enc_info"       value="<%=enc_info%>">
    <input type="hidden" name="enc_data"       value="<%=enc_data%>">
    <input type="hidden" name="use_pay_method" value="<%=use_pay_method%>">
    <input type="hidden" name="cash_tr_code"   value="<%=cash_tr_code%>">
    <!-- 추가 파라미터 -->
    <input type="hidden" name="param_opt_1"	   value="<%=param_opt_1%>">
    <input type="hidden" name="param_opt_2"	   value="<%=param_opt_2%>">
    <input type="hidden" name="param_opt_3"	   value="<%=param_opt_3%>">
</form>
</body>
</html>
