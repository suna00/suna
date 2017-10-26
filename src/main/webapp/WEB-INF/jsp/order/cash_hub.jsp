<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%
    /* ============================================================================== */
    /* =   PAGE : 등록/변경 처리 PAGE                                               = */
    /* = -------------------------------------------------------------------------- = */
    /* =   연동시 오류가 발생하는 경우 아래의 주소로 접속하셔서 확인하시기 바랍니다.= */
    /* =   접속 주소 : http://testpay.kcp.co.kr/pgsample/FAQ/search_error.jsp       = */
    /* = -------------------------------------------------------------------------- = */
    /* =   Copyright (c)  2007   KCP Inc.   All Rights Reserved.                    = */
    /* ============================================================================== */

    /* ============================================================================== */
    /* = 라이브러리 및 사이트 정보 include                                          = */
    /* = -------------------------------------------------------------------------- = */
%>

<%@ page import="com.kcp.*" %>
<%@ include file="./cfg/site_conf_inc.jsp" %>

<%!
    /* ============================================================================== */
    /* =   null 값을 처리하는 메소드                                                 = */
    /* = -------------------------------------------------------------------------- = */
        public String f_get_parm( String val )
        {
          if ( val == null ) val = "";
          return  val;
        }
    /* ============================================================================== */
%>

<%
    /* ============================================================================== */
    /* =   01. 요청 정보 설정                                                       = */
    /* = -------------------------------------------------------------------------- = */
    String res_cd       = "";                                                      // 결과코드
    String res_msg      = "";                                                      // 결과메시지
    String tx_cd        = "";                                                      // 트랜잭션 코드
    String bSucc        = "";                                                      // DB 작업 성공 여부
    /* = --------------------------------------------------------------------------= */
    String pay_type     = request.getParameter( "pay_type"     ) ;                 // 결제 서비스 구분
    String pay_trade_no = request.getParameter( "pay_trade_no" ) ;                 // 결제거래번호
    /* = --------------------------------------------------------------------------= */
    String req_tx       = request.getParameter( "req_tx"       ) ;                 // 요청 종류
    String trad_time    = request.getParameter( "trad_time"    ) ;                 // 원거래 시각
    /* = --------------------------------------------------------------------------= */
    String ordr_idxx    = request.getParameter( "ordr_idxx"    ) ;                 // 주문번호
    String buyr_name    = request.getParameter( "buyr_name"    ) ;                 // 주문자 이름
    String buyr_tel1    = request.getParameter( "buyr_tel1"    ) ;                 // 주문자 전화번호
    String buyr_tel2    = request.getParameter( "buyr_tel2"    ) ;                 // 주문자 전화번호
    String buyr_mail    = request.getParameter( "buyr_mail"    ) ;                 // 주문자 메일
    String good_name    = request.getParameter( "good_name"    ) ;                 // 주문상품명
    String comment      = request.getParameter( "comment"      ) ;                 // 비고
    /* = --------------------------------------------------------------------------= */
    String cash_no      = "" ;                                                     // 현금영수증 거래번호
    String receipt_no   = "" ;                                                     // 현금영수증 승인번호
    String app_time     = "" ;                                                     // 승인시간(YYYYMMDDhhmmss)
    String reg_stat     = "" ;                                                     // 등록 상태 코드
    String reg_desc     = "" ;                                                     // 등록 상태 설명
    /* = --------------------------------------------------------------------------= */
    String corp_type    = request.getParameter( "corp_type"    ) ;                 // 사업장 구분
    String corp_tax_type= request.getParameter( "corp_tax_type") ;                 // 과세/면세 구분
    String corp_tax_no  = request.getParameter( "corp_tax_no"  ) ;                 // 발행 사업자 번호
    String corp_nm      = request.getParameter( "corp_nm"      ) ;                 // 상호
    String corp_owner_nm= request.getParameter( "corp_owner_nm") ;                 // 대표자명
    String corp_addr    = request.getParameter( "corp_addr"    ) ;                 // 사업장주소
    String corp_telno   = request.getParameter( "corp_telno"   ) ;                 // 사업장 대표 연락처
    /* = --------------------------------------------------------------------------= */
    String user_type    = request.getParameter( "user_type"    ) ;                 // 사용자 구분
    String tr_code      = request.getParameter( "tr_code"      ) ;                 // 발행용도
    String id_info      = request.getParameter( "id_info"      ) ;                 // 신분확인 ID
    String amt_tot      = request.getParameter( "amt_tot"      ) ;                 // 거래금액 총 합
    String amt_sup      = request.getParameter( "amt_sup"      ) ;                 // 공급가액
    String amt_svc      = request.getParameter( "amt_svc"      ) ;                 // 봉사료
    String amt_tax      = request.getParameter( "amt_tax"      ) ;                 // 부가가치세
    /* = --------------------------------------------------------------------------= */
    String mod_type     = request.getParameter( "mod_type"     ) ;                 // 변경 타입
    String mod_value    = request.getParameter( "mod_value"    ) ;                 // 변경 요청 거래번호
    String mod_gubn     = request.getParameter( "mod_gubn"     ) ;                 // 변경 요청 거래번호 구분
    String mod_mny      = request.getParameter( "mod_mny"      ) ;                 // 변경 요청 금액
    String rem_mny      = request.getParameter( "rem_mny"      ) ;                 // 변경처리 이전 금액
   	/* ============================================================================== */


    /* ============================================================================== */
    /* =   02. 인스턴스 생성 및 초기화                                               = */
    /* = -------------------------------------------------------------------------- = */
    J_PP_CLI_N  c_PayPlus = new J_PP_CLI_N();

    c_PayPlus.mf_init( "", g_conf_gw_url, g_conf_gw_port, g_conf_tx_mode, g_conf_cash_log_dir );
    c_PayPlus.mf_init_set();
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   03. 처리 요청 정보 설정, 실행                                             = */
    /* = -------------------------------------------------------------------------- = */

    /* = -------------------------------------------------------------------------- = */
    /* =   03-1. 승인 요청                                                          = */
    /* = -------------------------------------------------------------------------- = */
    // 업체 환경 정보
    String cust_ip = request.getRemoteAddr();

    if ( req_tx.equals( "pay" ) )
    {

        tx_cd = "07010000" ; // 현금영수증 등록 요청

        int rcpt_data_set ;
        int ordr_data_set ;
        int corp_data_set ;

        rcpt_data_set   = c_PayPlus.mf_add_set( "rcpt_data" ) ;
        ordr_data_set   = c_PayPlus.mf_add_set( "ordr_data" ) ;
        corp_data_set   = c_PayPlus.mf_add_set( "corp_data" ) ;

        // 현금영수증 정보
        c_PayPlus.mf_set_us( rcpt_data_set, "user_type", g_conf_user_type ) ;
        c_PayPlus.mf_set_us( rcpt_data_set, "trad_time", trad_time ) ;
        c_PayPlus.mf_set_us( rcpt_data_set, "tr_code"  , tr_code   ) ;
        c_PayPlus.mf_set_us( rcpt_data_set, "id_info"  , id_info   ) ;
        c_PayPlus.mf_set_us( rcpt_data_set, "amt_tot"  , amt_tot   ) ;
        c_PayPlus.mf_set_us( rcpt_data_set, "amt_sup"  , amt_sup   ) ;
        c_PayPlus.mf_set_us( rcpt_data_set, "amt_svc"  , amt_svc   ) ;
        c_PayPlus.mf_set_us( rcpt_data_set, "amt_tax"  , amt_tax   ) ;
        c_PayPlus.mf_set_us( rcpt_data_set, "pay_type" , "PAXX"    ) ;

        // 주문 정보
        c_PayPlus.mf_set_us( ordr_data_set, "ordr_idxx", ordr_idxx ) ;
        c_PayPlus.mf_set_us( ordr_data_set, "good_name", good_name ) ;
        c_PayPlus.mf_set_us( ordr_data_set, "buyr_name", buyr_name ) ;
        c_PayPlus.mf_set_us( ordr_data_set, "buyr_tel1", buyr_tel1 ) ;
        c_PayPlus.mf_set_us( ordr_data_set, "buyr_mail", buyr_mail ) ;
        c_PayPlus.mf_set_us( ordr_data_set, "comment"  , comment   ) ;

        // 가맹점 정보
        c_PayPlus.mf_set_us( corp_data_set, "corp_type", corp_type ) ;

        // 입점몰인 경우 판매상점 DATA 전문 생성
        if( "1".equals( corp_type ) )
        {
            c_PayPlus.mf_set_us( corp_data_set, "corp_tax_type"   , corp_tax_type  ) ;
            c_PayPlus.mf_set_us( corp_data_set, "corp_tax_no"     , corp_tax_no    ) ;
            c_PayPlus.mf_set_us( corp_data_set, "corp_sell_tax_no", corp_tax_no    ) ;
            c_PayPlus.mf_set_us( corp_data_set, "corp_nm"         , corp_nm        ) ;
            c_PayPlus.mf_set_us( corp_data_set, "corp_owner_nm"   , corp_owner_nm  ) ;
            c_PayPlus.mf_set_us( corp_data_set, "corp_addr"       , corp_addr      ) ;
            c_PayPlus.mf_set_us( corp_data_set, "corp_telno"      , corp_telno     ) ;
        }

        c_PayPlus.mf_add_rs( ordr_data_set , rcpt_data_set ) ;
        c_PayPlus.mf_add_rs( ordr_data_set , corp_data_set ) ;
    }
    /* = -------------------------------------------------------------------------- = */
    /* =   03-2. 취소 요청                                                          = */
    /* = -------------------------------------------------------------------------- = */
    else if ( req_tx.equals( "mod" ) )
    {
        int     mod_data_set_no ;

        mod_data_set_no = c_PayPlus.mf_add_set( "mod_data" ) ;

        if( mod_type.equals( "STSQ" ) )
        {
            tx_cd = "07030000" ;     // 조회 요청
        }
        else
        {
            tx_cd = "07020000" ;     // 취소 요청
        }

        if( mod_type.equals( "STPC" ) )     // 부분 취소
        {
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_mny"  , mod_mny ) ;
            c_PayPlus.mf_set_us( mod_data_set_no, "rem_mny"  , rem_mny ) ;
        }

        c_PayPlus.mf_set_us( mod_data_set_no, "mod_type"  , mod_type  ) ;
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_value" , mod_value ) ;
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_gubn"  , mod_gubn  ) ;
        c_PayPlus.mf_set_us( mod_data_set_no, "trad_time" , trad_time ) ;
    }
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   03-3. 실행                                                               = */
    /* ------------------------------------------------------------------------------ */
    if ( tx_cd.length() > 0 )
    {
        c_PayPlus.mf_do_tx( g_conf_site_id, "", tx_cd, cust_ip, ordr_idxx, g_conf_log_level, "1" ) ;
    }
    else
    {
        c_PayPlus.m_res_cd  = "9562" ;
        c_PayPlus.m_res_msg = "연동 오류" ;
    }
    res_cd  = c_PayPlus.m_res_cd ;                           // 결과 코드
    res_msg = c_PayPlus.m_res_msg ;                          // 결과 메시지
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   04. 승인 결과 처리                                                       = */
    /* = -------------------------------------------------------------------------- = */
    if ( req_tx.equals( "pay" ) )
    {
        if ( res_cd.equals( "0000" ) )
        {
            cash_no    = c_PayPlus.mf_get_res( "cash_no"    ) ;     // 현금영수증 거래번호
            receipt_no = c_PayPlus.mf_get_res( "receipt_no" ) ;     // 현금영수증 승인번호
            app_time   = c_PayPlus.mf_get_res( "app_time"   ) ;     // 승인시간(YYYYMMDDhhmmss)
            reg_stat   = c_PayPlus.mf_get_res( "reg_stat"   ) ;     // 등록 상태 코드
            reg_desc   = c_PayPlus.mf_get_res( "reg_desc"   ) ;     // 등록 상태 설명

    /* = -------------------------------------------------------------------------- = */
    /* =   04-1. 승인 결과를 업체 자체적으로 DB 처리 작업하시는 부분입니다.           = */
    /* = -------------------------------------------------------------------------- = */
    /* =         승인 결과를 DB 작업 하는 과정에서 정상적으로 승인된 건에 대해         = */
    /* =         DB 작업을 실패하여 DB update 가 완료되지 않은 경우, 자동으로         = */
    /* =         승인 취소 요청을 하는 프로세스가 구성되어 있습니다.                  = */
    /* =         DB 작업이 실패 한 경우, bSucc 라는 변수(String)의 값을 "false"      = */
    /* =         로 세팅해 주시기 바랍니다. (DB 작업 성공의 경우에는 "false" 이외의   = */
    /* =         값을 세팅하시면 됩니다.)                                            = */
    /* = -------------------------------------------------------------------------- = */
            bSucc = "" ;             // DB 작업 실패일 경우 "false" 로 세팅

    /* = -------------------------------------------------------------------------- = */
    /* =   04-2. DB 작업 실패일 경우 자동 승인 취소                                  = */
    /* = -------------------------------------------------------------------------- = */
            if ( bSucc.equals( "false" ) )
            {
                int mod_data_set_no ;

                mod_data_set_no = c_PayPlus.mf_add_set( "mod_data" ) ;

                tx_cd = "07020000" ;	// 취소 요청

                c_PayPlus.mf_set_us( mod_data_set_no, "mod_type" ,  "STSC"   ) ;
                c_PayPlus.mf_set_us( mod_data_set_no, "mod_value", cash_no   ) ;
                c_PayPlus.mf_set_us( mod_data_set_no, "mod_gubn" ,  "MG01"   ) ;
                c_PayPlus.mf_set_us( mod_data_set_no, "trad_time", trad_time ) ;

                c_PayPlus.mf_do_tx( g_conf_site_id, "", tx_cd, cust_ip, ordr_idxx, g_conf_log_level, "0" ) ;

                res_cd  = c_PayPlus.m_res_cd ;
                res_msg = c_PayPlus.m_res_msg ;

            }
            // End of [res_cd = "0000"]

        }
        /* = -------------------------------------------------------------------------- = */
        /* =   04-3. 등록 실패를 업체 자체적으로 DB 처리 작업하시는 부분입니다.            = */
        /* = -------------------------------------------------------------------------- = */
        else
        {
        }
    /* ============================================================================== */
    }

    /* ============================================================================== */
    /* =   05. 변경 결과 처리                                                       = */
    /* = -------------------------------------------------------------------------- = */
    else if ( req_tx.equals ( "mod" ) )
    {
        if ( res_cd.equals ( "0000" ) )
        {
            cash_no    = c_PayPlus.mf_get_res( "cash_no"    ) ;  // 현금영수증 거래번호
            receipt_no = c_PayPlus.mf_get_res( "receipt_no" ) ;  // 현금영수증 승인번호
            app_time   = c_PayPlus.mf_get_res( "app_time"   ) ;  // 승인시간(YYYYMMDDhhmmss)
            reg_stat   = c_PayPlus.mf_get_res( "reg_stat"   ) ;  // 등록 상태 코드
            reg_desc   = c_PayPlus.mf_get_res( "reg_desc"   ) ;  // 등록 상태 설명
            amt_tot    = c_PayPlus.mf_get_res( "amt_tot"    ) ;  // 거래금액 총 합
            ordr_idxx  = c_PayPlus.mf_get_res( "ordr_idxx"  ) ;  // 주문번호
        }
    }
    /* = -------------------------------------------------------------------------- = */
    /* =   05-1. 변경 실패를 업체 자체적으로 DB 처리 작업하시는 부분입니다.            = */
    /* = -------------------------------------------------------------------------- = */
    else
    {
    }
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   07. 폼 구성 및 결과페이지 호출                                            = */
    /* ============================================================================== */
%>
    <html>
    <head>
    <script language = "javascript">
        function goResult()
        {
            document.pay_info.submit();
        }
    </script>
    </head>

    <body onload="goResult();">
    <form name="pay_info" method="post" action="./result.jsp">
        <input type="hidden" name="req_tx"            value="<%= req_tx        %>">     <!-- 요청 구분 -->
        <input type="hidden" name="bSucc"             value="<%= bSucc         %>">     <!-- 쇼핑몰 DB 처리 성공 여부 -->

        <input type="hidden" name="res_cd"            value="<%= res_cd        %>">     <!-- 결과 코드 -->
        <input type="hidden" name="res_msg"           value="<%= res_msg       %>">     <!-- 결과 메세지 -->
        <input type="hidden" name="ordr_idxx"         value="<%= ordr_idxx     %>">     <!-- 주문번호 -->
        <input type="hidden" name="good_name"         value="<%= good_name     %>">     <!-- 상품명 -->
        <input type="hidden" name="buyr_name"         value="<%= buyr_name     %>">     <!-- 주문자명 -->
        <input type="hidden" name="buyr_tel1"         value="<%= buyr_tel1     %>">     <!-- 주문자 전화번호 -->
        <input type="hidden" name="buyr_mail"         value="<%= buyr_mail     %>">     <!-- 주문자 E-mail -->
        <input type="hidden" name="comment"           value="<%= comment       %>">     <!-- 비고 -->

        <input type="hidden" name="corp_type"         value="<%= corp_type     %>">     <!-- 사업장 구분 -->
        <input type="hidden" name="corp_tax_type"     value="<%= corp_tax_type %>">     <!-- 과세/면세 구분 -->
        <input type="hidden" name="corp_tax_no"       value="<%= corp_tax_no   %>">     <!-- 발행 사업자 번호 -->
        <input type="hidden" name="corp_nm"           value="<%= corp_nm       %>">     <!-- 상호 -->
        <input type="hidden" name="corp_owner_nm"     value="<%= corp_owner_nm %>">     <!-- 대표자명 -->
        <input type="hidden" name="corp_addr"         value="<%= corp_addr     %>">     <!-- 사업장주소 -->
        <input type="hidden" name="corp_telno"        value="<%= corp_telno    %>">     <!-- 사업장 대표 연락처 -->

        <input type="hidden" name="user_type"         value="<%= user_type     %>">     <!-- 사용자 구분 -->
        <input type="hidden" name="tr_code"           value="<%= tr_code       %>">     <!-- 발행용도 -->
        <input type="hidden" name="id_info"           value="<%= id_info       %>">     <!-- 신분확인 ID -->
        <input type="hidden" name="amt_tot"           value="<%= amt_tot       %>">     <!-- 거래금액 총 합 -->
        <input type="hidden" name="amt_sub"           value="<%= amt_sup       %>">     <!-- 공급가액 -->
        <input type="hidden" name="amt_svc"           value="<%= amt_svc       %>">     <!-- 봉사료 -->
        <input type="hidden" name="amt_tax"           value="<%= amt_tax       %>">     <!-- 부가가치세 -->
        <input type="hidden" name="pay_type"          value="<%= pay_type      %>">     <!-- 결제 서비스 구분 -->
        <input type="hidden" name="pay_trade_no"      value="<%= pay_trade_no  %>">     <!-- 결제 거래번호 -->

        <input type="hidden" name="mod_type"          value="<%= mod_type      %>">     <!-- 변경 타입 -->
        <input type="hidden" name="mod_value"         value="<%= mod_value     %>">     <!-- 변경 요청 거래번호 -->
        <input type="hidden" name="mod_gubn"          value="<%= mod_gubn      %>">     <!-- 변경 요청 거래번호 구분 -->
        <input type="hidden" name="mod_mny"           value="<%= mod_mny       %>">     <!-- 변경 요청 금액 -->
        <input type="hidden" name="rem_mny"           value="<%= rem_mny       %>">     <!-- 변경처리 이전 금액 -->

		<input type="hidden" name="cash_no"           value="<%= cash_no       %>">     <!-- 현금영수증 거래번호 -->
        <input type="hidden" name="receipt_no"        value="<%= receipt_no    %>">     <!-- 현금영수증 승인번호 -->
        <input type="hidden" name="app_time"          value="<%= app_time      %>">     <!-- 승인시간(YYYYMMDDhhmmss) -->
        <input type="hidden" name="reg_stat"          value="<%= reg_stat      %>">     <!-- 등록 상태 코드 -->
        <input type="hidden" name="reg_desc"          value="<%= reg_desc      %>">     <!-- 등록 상태 설명 -->
    </form>
    </body>
    </html>