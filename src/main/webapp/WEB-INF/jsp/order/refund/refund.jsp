<%@ page language="java" contentType="text/html;charset=euc-kr"%>
<%
    /* ============================================================================== */
    /* =   PAGE : �������� ��û �� ��� ó�� PAGE                                   = */
    /* = -------------------------------------------------------------------------- = */
    /* =   ������ ������ �߻��ϴ� ��� �Ʒ��� �ּҷ� �����ϼż� Ȯ���Ͻñ� �ٶ��ϴ�.= */
    /* =   ���� �ּ� : http://kcp.co.kr/technique.requestcode.do                    = */
    /* = -------------------------------------------------------------------------- = */
    /* =   Copyright (c)  2013   KCP Inc.   All Rights Reserverd.                   = */
    /* ============================================================================== */
%>
<%
    /* ============================================================================== */
    /* = ���̺귯�� �� ����Ʈ ���� include                                          = */
    /* = -------------------------------------------------------------------------- = */
%>
<%@ page import="com.kcp.*"%>
<%@ page import="java.net.URLEncoder"%>
<%@ include file = "../cfg/site_conf_inc.jsp"%>
<%
    /* = -------------------------------------------------------------------------- = */
    /* =   ȯ�� ���� ���� Include END                                               = */
    /* ============================================================================== */
%>

<%!
    /* ============================================================================== */
    /* =   null ���� ó���ϴ� �޼ҵ�                                                = */
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
    /* =   POST ���� üũ�κ�                                                       = */
    /* = -------------------------------------------------------------------------- = */
    if (!request.getMethod().equals("POST"))
    {
        out.println("�߸��� ��η� �����Ͽ����ϴ�.");
        return;
    }
    /* ============================================================================== */
%>

<%
    /* ============================================================================== */

    request.setCharacterEncoding ( "euc-kr" ) ;
    /* ============================================================================== */
    /* =   01. ���� ��û ���� ����                                                  = */
    /* = -------------------------------------------------------------------------- = */
    String cust_ip      = request.getRemoteAddr();                                   // ��û IP
    String req_tx       = f_get_parm( request.getParameter( "req_tx"       ) );      // ��û ����
    String tran_cd      = "";                                                        // Ʈ����� �ڵ�
    /* = -------------------------------------------------------------------------- = */
    String res_cd       = "";                                                        // ����ڵ�
    String res_msg      = "";                                                        // ����޽���
    String app_time     = "";                                                        // �����ð�
    /* = -------------------------------------------------------------------------- = */
    String mod_type      = f_get_parm( request.getParameter( "mod_type"      ) );     // ��������
    String mod_desc      = f_get_parm( request.getParameter( "mod_desc"      ) );     // ��������
    String tno           = f_get_parm( request.getParameter( "tno"           ) );     // KCP �ŷ���ȣ
    String mod_mny       = f_get_parm( request.getParameter( "mod_mny"       ) );     // ȯ�ұݾ�
    String rem_mny       = f_get_parm( request.getParameter( "rem_mny"       ) );     // ȯ�� �� �ݾ�
    String mod_bankcode  = f_get_parm( request.getParameter( "mod_bankcode"  ) );     // ���� �ڵ�
    String mod_account   = f_get_parm( request.getParameter( "mod_account"   ) );     // �߱� ����
    String mod_depositor = f_get_parm( request.getParameter( "mod_depositor" ) );     // ������
    String mod_comp_type = f_get_parm( request.getParameter( "mod_comp_type" ) );     // ���� �ڵ�
    String mod_socno     = f_get_parm( request.getParameter( "mod_socno"     ) );     // �߱� ����
    String mod_socname   = f_get_parm( request.getParameter( "mod_socname"   ) );     // ������
    /* = -------------------------------------------------------------------------- = */

    /* ============================================================================== */
    /* =   02. �ν��Ͻ� ���� �� �ʱ�ȭ                                              = */
    /* = -------------------------------------------------------------------------- = */
    J_PP_CLI_N c_PayPlus = new J_PP_CLI_N();

    c_PayPlus.mf_init( "", g_conf_gw_url, g_conf_gw_port, g_conf_tx_mode, g_conf_log_dir );
    c_PayPlus.mf_init_set();
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   03. ó�� ��û ���� ����, ����                                            = */
    /* = -------------------------------------------------------------------------- = */

    /* = -------------------------------------------------------------------------- = */
    /* =   03-1. ���� ��û                                                          = */
    /* = -------------------------------------------------------------------------- = */
    // ��ü ȯ�� ����

    if ( req_tx.equals("mod") )
    {
        int     mod_data_set_no;

        tran_cd = "00200000";
        mod_data_set_no = c_PayPlus.mf_add_set( "mod_data" );

        c_PayPlus.mf_set_us( mod_data_set_no, "mod_type",  mod_type              );     // ���ŷ� ���� ��û ����
        c_PayPlus.mf_set_us( mod_data_set_no, "tno",       tno                   );     // �ŷ���ȣ
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_ip",    cust_ip               );     // ���� ��û�� IP
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_desc",  mod_desc              );     // ���� ����

        c_PayPlus.mf_set_us( mod_data_set_no, "mod_bankcode",   mod_bankcode     );     // ȯ�� ��û ���� �ڵ�
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_account",    mod_account      );     // ȯ�� ��û ����
        c_PayPlus.mf_set_us( mod_data_set_no, "mod_depositor",  mod_depositor    );     // ȯ�� ��û �����ָ�

        if ( mod_type.equals("STHD") )
        {
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_sub_type",   "MDSC00"        );      // ���� ����
        }
        else if ( mod_type.equals("STPD") )
        {
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_sub_type",   "MDSC03"        );      // ���� ����
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_mny",        mod_mny         );      // ȯ�� ��û �ݾ�
            c_PayPlus.mf_set_us( mod_data_set_no, "rem_mny",        rem_mny         );      // ȯ�� �� �ݾ�
        }

        if (mod_comp_type.equals("MDCP01"))
        {
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_comp_type",   "MDCP01"        );      // ���� ����
        }
        else if (mod_comp_type.equals("MDCP02"))
        {
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_comp_type",   "MDCP02"        );      // ���� ����
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_socno",       mod_socno       );      // �Ǹ�Ȯ�� �ֹι�ȣ
            c_PayPlus.mf_set_us( mod_data_set_no, "mod_socname",     mod_socname     );      // �Ǹ�Ȯ�� ����
        }
    }
    /* ============================================================================== */

    /* ============================================================================== */
    /* =   04. ����                                                                 = */
    /* ------------------------------------------------------------------------------ */
    if ( tran_cd.length() > 0 )
    {
        c_PayPlus.mf_do_tx( g_conf_site_cd, g_conf_site_key, tran_cd, cust_ip, "", "3", "0" );
    }
    else
    {
        c_PayPlus.m_res_cd  = "9562";
        c_PayPlus.m_res_msg = "���� ����";
    }
    res_cd  = c_PayPlus.m_res_cd;                      // ��� �ڵ�
    res_msg = c_PayPlus.m_res_msg;                     // ��� �޽���
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   05. ������� ȯ�� ��� ó��                                              = */
    /* = -------------------------------------------------------------------------- = */
    if ( req_tx == "mod" )
    {
        if ( res_cd == "0000" )
        {
            tno = c_PayPlus.mf_get_res( "tno" );       // KCP �ŷ� ���� ��ȣ
        }
        else if ( res_cd != "0000" )
        {

        }
    }      /* End of Process */

    /* ============================================================================== */
    /* =   07. �� ���� �� ��������� ȣ��                                           = */
    /* ============================================================================== */

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=EUC-KR">
    <title>*** KCP Online Payment System Ver 6.0 [HUB Version] ***</title>
    <script type="text/javascript">
        function goResult()
        {
            document.pay_info.submit();
        }

        // ���� �� ���ΰ�ħ ���� ���� ��ũ��Ʈ
        function noRefresh()
        {
            /* CTRL + NŰ ����. */
            if ((event.keyCode == 78) && (event.ctrlKey == true))
            {
                event.keyCode = 0;
                return false;
            }
            /* F5 ��Ű ����. */
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
    <input type="hidden" name="req_tx"         value="<%= req_tx        %>">     <!--   ��û ����     -->
    <input type="hidden" name="res_cd"         value="<%= res_cd        %>">     <!--   ��� �ڵ�     -->
    <input type="hidden" name="res_msg"        value="<%= res_msg       %>">     <!--   ��� �޼���   -->
    <input type="hidden" name="app_time"       value="<%= app_time      %>">     <!--   �����ð�      -->
    <input type="hidden" name="mod_account"    value="<%= mod_account   %>">     <!--   ȯ�Ұ��¹�ȣ  -->
    <input type="hidden" name="mod_depositor"  value="<%= mod_depositor %>">     <!--   �����ָ�      -->
    <input type="hidden" name="mod_bankcode"   value="<%= mod_bankcode  %>">     <!--   ȯ�������ڵ�  -->
</form>
</body>
</html>