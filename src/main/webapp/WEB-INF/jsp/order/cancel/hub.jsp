<%@ page language="java" contentType="text/html;charset=euc-kr"%>
<%
    /* ============================================================================== */
    /* =   PAGE : 지불 요청 및 결과 처리 PAGE                                       = */
    /* = -------------------------------------------------------------------------- = */
    /* =   연동시 오류가 발생하는 경우 아래의 주소로 접속하셔서 확인하시기 바랍니다.= */
    /* =   접속 주소 : http://kcp.co.kr/technique.requestcode.do                    = */
    /* = -------------------------------------------------------------------------- = */
    /* =   Copyright (c)  2016   NHN KCP Inc.   All Rights Reserverd.               = */
    /* ============================================================================== */

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
    /* =   환경 설정 파일 Include                                                   = */
    /* = -------------------------------------------------------------------------- = */
    /* =   ※ 필수                                                                  = */
    /* =   테스트 및 실결제 연동시 site_conf_inc.jsp파일을 수정하시기 바랍니다.     = */
    /* = -------------------------------------------------------------------------- = */
%>
<%@ page import="com.kcp.*" %>
<%@ page import="java.net.URLEncoder"%>
<%@ include file="../cfg/site_conf_inc.jsp" %>
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
    request.setCharacterEncoding("utf-8");
    /* ============================================================================== */
    /* =   02. 지불 요청 정보 설정                                                  = */
    /* = -------------------------------------------------------------------------- = */
    String req_tx         = f_get_parm(request.getParameter("req_tx"));             // 취소요청
    String tran_cd        = f_get_parm(request.getParameter("tran_cd"));            // 업무코드
    String cust_ip        = f_get_parm( request.getRemoteAddr()                  ); // 요청 IP
    /* = -------------------------------------------------------------------------- = */
    String res_cd         = "";                                                     // 응답코드
    String res_msg        = "";                                                     // 응답 메세지
    String tno            = f_get_parm(request.getParameter("tno"));                // KCP 거래 고유 번호
    /* = -------------------------------------------------------------------------- = */
    String mod_type       = f_get_parm(request.getParameter("mod_type"));           // 변경TYPE(승인취소시 필요)
    String mod_desc       = f_get_parm(request.getParameter("mod_desc"));           // 변경사유
    String panc_mod_mny   = f_get_parm(request.getParameter("mod_mny"));            // 부분취소 금액
    String panc_rem_mny   = f_get_parm(request.getParameter("rem_mny"));            // 부분취소 가능 금액
    String mod_tax_mny    = f_get_parm(request.getParameter("mod_tax_mny"));        // 공급가 부분 취소 요청 금액
    String mod_vat_mny    = f_get_parm(request.getParameter("mod_vat_mny"));        // 부과세 부분 취소 요청 금액
    String mod_free_mny   = f_get_parm(request.getParameter("mod_free_mny"));       // 비과세 부분 취소 요청 금액
    /* ============================================================================== */
    /* =   02. 지불 요청 정보 설정 END                                              = */
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   03. 인스턴스 생성 및 초기화(변경 불가)                                   = */
    /* = -------------------------------------------------------------------------- = */
    /* =       결제에 필요한 인스턴스를 생성하고 초기화 합니다.                     = */
    /* = -------------------------------------------------------------------------- = */
    J_PP_CLI_N c_PayPlus = new J_PP_CLI_N();

    c_PayPlus.mf_init( "", g_conf_gw_url, g_conf_gw_port, g_conf_tx_mode, g_conf_cancel_log_dir);
    c_PayPlus.mf_init_set();

    /* ============================================================================== */
    /* =   03. 인스턴스 생성 및 초기화 END                                          = */
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   04. 처리 요청 정보 설정                                                  = */
    /* = -------------------------------------------------------------------------- = */

    /* = -------------------------------------------------------------------------- = */
    /* =   04-1. 취소/매입 요청                                                     = */
    /* = -------------------------------------------------------------------------- = */
    if ( req_tx.equals( "" ) )
    {
        int    mod_data_set_no;

        tran_cd = "00200000";
        mod_data_set_no = c_PayPlus.mf_add_set( "mod_data" );

//        tno = "17563910507190";
//        mod_type = "STSC";

        c_PayPlus.mf_set_us( mod_data_set_no, "tno",        tno        ); // KCP 원거래 거래번호
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_type",   mod_type      ); // 전체취소 STSC / 부분취소 STPC
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_ip",     cust_ip     ); // 변경 요청자 IP
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_desc",   mod_desc          ); // 변경 사유

        if ( mod_type.equals( "STPC" ) ) // 부분취소의 경우
        {
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_mny", panc_mod_mny ); // 취소요청금액
            c_PayPlus.mf_set_us( mod_data_set_no, "rem_mny", panc_rem_mny ); // 취소가능잔액

            //복합거래 부분 취소시 주석을 풀어 주시기 바랍니다.
            //c_PayPlus.mf_set_us( mod_data_set_no, "tax_flag",     "TG03"                       ); // 복합과세 구분
            //c_PayPlus.mf_set_us( mod_data_set_no, "mod_tax_mny",  mod_tax_mny                  ); // 공급가 부분 취소 요청 금액
            //c_PayPlus.mf_set_us( mod_data_set_no, "mod_vat_mny",  mod_vat_mny                  ); // 부과세 부분 취소 요청 금액
            //c_PayPlus.mf_set_us( mod_data_set_no, "mod_free_mny", mod_free_mny                 ); // 비과세 부분 취소 요청 금액
        }
    }
    /* = -------------------------------------------------------------------------- = */
    /* =   04. 처리 요청 정보 설정 END                                              = */
    /* = ========================================================================== = */


    /* = ========================================================================== = */
    /* =   05. 실행                                                                 = */
    /* = -------------------------------------------------------------------------- = */
    if ( tran_cd.length() > 0 )
    {
        c_PayPlus.mf_do_tx( g_conf_site_cd, g_conf_site_key, tran_cd, "", "", g_conf_log_level, "1" );

        res_cd  = c_PayPlus.m_res_cd;  // 결과 코드
        res_msg = c_PayPlus.m_res_msg; // 결과 메시지
    }
    else
    {
        c_PayPlus.m_res_cd  = "9562";
        c_PayPlus.m_res_msg = "연동 오류";
    }

    /* = -------------------------------------------------------------------------- = */
    /* =   05. 실행 END                                                             = */
    /* ============================================================================== */

    if ( res_cd.equals( "0000" ) ) // 정상결제 인 경우
    {
        out.println( "취소요청이 완료되었습니다.      <br>");
        out.println( "결과코드 : "      + res_cd   + "<br>");
        out.println( "결과메세지 : "    + res_msg  + "<p>");
    }
    else
    {
        out.println( "취소요청이 처리 되지 못하였습니다.  <br>");
        out.println( "결과코드 : "      + res_cd       + "<br>");
        out.println( "결과메세지 : "    + res_msg      + "<p>");
    }

    System.out.println("req_tx : " + req_tx);
    System.out.println("mod_type : " + mod_type);
    System.out.println("tno : " + tno);
    System.out.println("mod_desc : " + mod_desc);
    System.out.println("panc_mod_mny : " + panc_mod_mny);
    System.out.println("panc_rem_mny : " + panc_rem_mny);

    /* = -------------------------------------------------------------------------- = */
    /* =   06. 취소 결과 처리 END                                                   = */
    /* ============================================================================== */
%>