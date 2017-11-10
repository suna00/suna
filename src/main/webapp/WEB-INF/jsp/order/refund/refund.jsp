<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    /* ============================================================================== */
    /* =   PAGE : 계좌인증 요청 및 결과 처리 PAGE                                   = */
    /* = -------------------------------------------------------------------------- = */
    /* =   연동시 오류가 발생하는 경우 아래의 주소로 접속하셔서 확인하시기 바랍니다.= */
    /* =   접속 주소 : http://kcp.co.kr/technique.requestcode.do                    = */
    /* = -------------------------------------------------------------------------- = */
    /* =   Copyright (c)  2013   KCP Inc.   All Rights Reserverd.                   = */
    /* ============================================================================== */
%>
<%
    /* ============================================================================== */
    /* = 라이브러리 및 사이트 정보 include                                          = */
    /* = -------------------------------------------------------------------------- = */
%>
<%@ page import="com.kcp.*"%>
<%@ page import="java.net.URLEncoder"%>
<%@ page import="java.net.URLDecoder" %>
<%@ include file = "../cfg/site_conf_inc.jsp"%>
<%
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
    /* ============================================================================== */
    /* =   POST 형식 체크부분                                                       = */
    /* = -------------------------------------------------------------------------- = */
    if (!request.getMethod().equals("POST"))
    {
        out.println("잘못된 경로로 접속하였습니다.");
        return;
    }
    /* ============================================================================== */
%>

<%
    /* ============================================================================== */

    request.setCharacterEncoding("UTF-8");
    /* ============================================================================== */
    /* =   01. 지불 요청 정보 설정                                                  = */
    /* = -------------------------------------------------------------------------- = */
    String cust_ip      = request.getRemoteAddr();                                   // 요청 IP
    String req_tx       = f_get_parm( request.getParameter( "req_tx"       ) );      // 요청 종류
    String tran_cd      = "";                                                        // 트랜잭션 코드
    /* = -------------------------------------------------------------------------- = */
    String res_cd       = "";                                                        // 결과코드
    String res_msg      = "";                                                        // 결과메시지
    String app_time     = "";                                                        // 인증시간
    /* = -------------------------------------------------------------------------- = */
    String mod_type      = f_get_parm( request.getParameter( "mod_type"      ) );     // 변경유형
    String mod_desc      = f_get_parm( request.getParameter( "mod_desc"      ) );     // 변경유형
    String tno           = f_get_parm( request.getParameter( "tno"           ) );     // KCP 거래번호
    String mod_mny       = f_get_parm( request.getParameter( "mod_mny"       ) );     // 환불금액
    String rem_mny       = f_get_parm( request.getParameter( "rem_mny"       ) );     // 환불 전 금액
    String mod_bankcode  = f_get_parm( request.getParameter( "mod_bankcode"  ) );     // 은행 코드
    String mod_account   = f_get_parm( request.getParameter( "mod_account"   ) );     // 발급 계좌
    String mod_depositor = f_get_parm( request.getParameter( "mod_depositor" ) );     // 예금주
    String mod_comp_type = f_get_parm( request.getParameter( "mod_comp_type" ) );     // 은행 코드
    String mod_socno     = f_get_parm( request.getParameter( "mod_socno"     ) );     // 발급 계좌
    String mod_socname   = f_get_parm( request.getParameter( "mod_socname"   ) );     // 예금주
    /* = -------------------------------------------------------------------------- = */

    /* ============================================================================== */
    /* =   02. 인스턴스 생성 및 초기화                                              = */
    /* = -------------------------------------------------------------------------- = */
    J_PP_CLI_N c_PayPlus = new J_PP_CLI_N();

    c_PayPlus.mf_init( "", g_conf_gw_url, g_conf_gw_port, g_conf_tx_mode, g_conf_log_dir );
    c_PayPlus.mf_init_set();
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   03. 처리 요청 정보 설정, 실행                                            = */
    /* = -------------------------------------------------------------------------- = */

    /* = -------------------------------------------------------------------------- = */
    /* =   03-1. 승인 요청                                                          = */
    /* = -------------------------------------------------------------------------- = */
    // 업체 환경 정보

    if ( req_tx.equals("mod") )
    {
        int     mod_data_set_no;

        tran_cd = "00200000";
        mod_data_set_no = c_PayPlus.mf_add_set( "mod_data" );

        c_PayPlus.mf_set_us( mod_data_set_no, "mod_type",  mod_type              );     // 원거래 변경 요청 종류
        c_PayPlus.mf_set_us( mod_data_set_no, "tno",       tno                   );     // 거래번호
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_ip",    cust_ip               );     // 변경 요청자 IP
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_desc",  mod_desc              );     // 변경 사유

        c_PayPlus.mf_set_us( mod_data_set_no, "mod_bankcode",   mod_bankcode     );     // 환불 요청 은행 코드
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_account",    mod_account      );     // 환불 요청 계좌
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_depositor",  mod_depositor    );     // 환불 요청 계좌주명

        if ( mod_type.equals("STHD") )
        {
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_sub_type",   "MDSC00"        );      // 변경 유형
        }
        else if ( mod_type.equals("STPD") )
        {
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_sub_type",   "MDSC03"        );      // 변경 유형
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_mny",        mod_mny         );      // 환불 요청 금액
            c_PayPlus.mf_set_us( mod_data_set_no, "rem_mny",        rem_mny         );      // 환불 전 금액
        }

        if (mod_comp_type.equals("MDCP01"))
        {
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_comp_type",   "MDCP01"        );      // 변경 유형
        }
        else if (mod_comp_type.equals("MDCP02"))
        {
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_comp_type",   "MDCP02"        );      // 변경 유형
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_socno",       mod_socno       );      // 실명확인 주민번호
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_socname",     mod_socname     );      // 실명확인 성명
        }
    }
    /* ============================================================================== */

    /* ============================================================================== */
    /* =   04. 실행                                                                 = */
    /* ------------------------------------------------------------------------------ */
    if ( tran_cd.length() > 0 )
    {
        c_PayPlus.mf_do_tx( g_conf_site_cd, g_conf_site_key, tran_cd, cust_ip, "", "3", "1" );
    }
    else
    {
        c_PayPlus.m_res_cd  = "9562";
        c_PayPlus.m_res_msg = "연동 오류";
    }
    res_cd  = c_PayPlus.m_res_cd;                      // 결과 코드
    res_msg = c_PayPlus.m_res_msg;                     // 결과 메시지
    System.out.println("#############################################################");
    System.out.println(c_PayPlus.getLogRecvMsg());
    System.out.println(new String(res_msg.getBytes("KSC5601"), "UTF-8"));
    System.out.println("#############################################################");
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   05. 가상계좌 환불 결과 처리                                              = */
    /* = -------------------------------------------------------------------------- = */
    if ( req_tx == "mod" )
    {
        if ( res_cd == "0000" )
        {
            tno = c_PayPlus.mf_get_res( "tno" );       // KCP 거래 고유 번호
        }
        else if ( res_cd != "0000" )
        {

        }
    }      /* End of Process */

    /* ============================================================================== */
    /* =   07. 폼 구성 및 결과페이지 호출                                           = */
    /* ============================================================================== */

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>*** KCP Online Payment System Ver 6.0 [HUB Version] ***</title>
    <script type="text/javascript">
        function goResult()
        {
            document.pay_info.submit();
        }

        // 결제 중 새로고침 방지 샘플 스크립트
        function noRefresh()
        {
            /* CTRL + N키 막음. */
            if ((event.keyCode == 78) && (event.ctrlKey == true))
            {
                event.keyCode = 0;
                return false;
            }
            /* F5 번키 막음. */
            if(event.keyCode == 116)
            {
                event.keyCode = 0;
                return false;
            }
        }

        document.onkeydown = noRefresh ;
    </script>
</head>

<body onload="goResult();" >

<form name="pay_info" method="post" action="/order/refundTest2">
    <input type="hidden" name="req_tx"         value="<%= req_tx        %>">     <!--   요청 구분     -->
    <input type="hidden" name="res_cd"         value="<%= res_cd        %>">     <!--   결과 코드     -->
    <input type="hidden" name="res_msg"        value="<%= res_msg       %>">     <!--   결과 메세지   -->
    <input type="hidden" name="app_time"       value="<%= app_time      %>">     <!--   인증시간      -->
    <input type="hidden" name="mod_account"    value="<%= mod_account   %>">     <!--   환불계좌번호  -->
    <input type="hidden" name="mod_depositor"  value="<%= mod_depositor %>">     <!--   예금주명      -->
    <input type="hidden" name="mod_bankcode"   value="<%= mod_bankcode  %>">     <!--   환불은행코드  -->
</form>
</body>
</html>