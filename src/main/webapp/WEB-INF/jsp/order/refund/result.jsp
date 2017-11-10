<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    /* ============================================================================== */
    /* =   PAGE : 계좌 인증 결과 PAGE                                               = */
    /* = -------------------------------------------------------------------------- = */
    /* =   Copyright (c)  2013   KCP Inc.   All Rights Reserved.                    = */
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
    request.setCharacterEncoding("UTF-8");
    /* ============================================================================== */
    /* =   지불 결과                                                                = */
    /* = -------------------------------------------------------------------------- = */
    String res_cd          = request.getParameter( "res_cd"         );      // 결과 코드
    String res_msg         = request.getParameter( "res_msg"        );      // 결과 메시지
    String app_time        = request.getParameter( "app_time"       );      // 인증시간
    /* = -------------------------------------------------------------------------- = */
    String mod_account     = request.getParameter( "mod_account"    );      // 환불 받을 계좌
    String mod_depositor   = request.getParameter( "mod_depositor"  );      // 환불 받을 계좌주명
    String mod_bankcode    = request.getParameter( "mod_bankcode"   );      // 환불 받을 은행코드
   /* = -------------------------------------------------------------------------- = */
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" >

<head>
    <title>*** KCP Online Payment System Ver 7.0[HUB Version] ***</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <style type="text/css">
        /*common*/
        *{margin:0; padding:0;}
        img {border:0px; margin:0px; padding:0px; vertical-align:top;}
        fieldset {border:0;}
        fieldset legend, caption {display:none;}
        ul, ol { list-style:none; margin:0; padding:0;}
        body, table, div, input, ul, li, dl, dt, dd, ol, p, h1, h2, h3, h4, h5, h6, form, fieldset, legend {margin:0; padding:0;}
        body {color:#222; font-family:tahoma; font-size:11px; font-weight:normal;}
        a {text-decoration:none; color:inherit;}
        table {border-collapse:collapse;}

        /*index*/
        #sample_index {width:400px; height:400px; position:absolute; background:#0e66a4; left:50%; top:50%; margin:-300px 0 0 -200px; overflow:hidden;}
        #sample_index h1 {font-size:20px; color:#fff; text-align:center; margin:25px 15px 15px; padding-bottom:25px; border-bottom:1px solid #60aee6;}
        #sample_index .btnSet {margin:30px 50px 0;}
        #sample_index a {position:relative; font-family:HY견고딕; display:inline-block; color:#fff;  font-size:16px; width:270px; text-align:leftr; padding:25px 0 20px 20px; margin-bottom:20px; cursor: pointer;
            -webkit-box-shadow: 5px 5px 7px 0px rgba(5, 50, 70, 0.5);
            -o-box-shadow: 5px 5px 7px 0px rgba(5, 50, 70, 0.5);
            box-shadow: 5px 5px 7px 0px rgba(5, 50, 70, 0.5);
        }
        #sample_index a.btn1 {background:#2cd740; left:15px;}
        #sample_index a.btn1:hover {background:#000; color:#2cd78c;}
        #sample_index a.btn2 {background:#18daa1; left:15px;}
        #sample_index a.btn2:hover {background:#000; color:#18daf1;}
        #sample_index a.btn3 {background:#3ca4ef; left:15px;}
        #sample_index a.btn3:hover {background:#000; color:#3ca4ff;}
        #sample_index a span {font-size:40px; position:absolute; right:20px; top:15px; }
        #sample_index .footer {margin-top:5px; font-size:11px; text-align:center; color:#000;}


        /*layout*/
        #sample_wrap {width:100%;}
        .sample {margin:0 auto; width:498px; border-left:1px solid #0e66a4; border-right:1px solid #0e66a4; border-bottom:1px solid #0e66a4;}

        /*style*/
        #sample_wrap h1 {width:500px; margin:50px auto 0; font-size:12px; background:#0e66a4; color:#fff; height:35px; line-height:35px; text-align:center;}
        #sample_wrap h1 span {font-weight:normal;}
        #sample_wrap p {padding:15px; line-height:16px;  color:#4383b0;}
        #sample_wrap p span {font-weight:bold; color:#e44541;}
        #sample_wrap h2 {font-size:12px; margin-left:15px;}
        #sample_wrap .tbl {width:470px; margin:5px 0 20px 15px;}
        #sample_wrap .tbl th, #sample_wrap .tbl td {text-align:left; padding-left:20px; border-top:1px solid #c1d4e2; border-bottom:1px solid #c1d4e2;}#sample_wrap .tbl th {background:#e8eef3; color:#4383b0; font-weight:normal; width:30%;}
        #sample_wrap .tbl td {width:70%; padding:5px 0 5px 10px;}
        #sample_wrap .tbl td select {width:100px; font-size:11px;}
        #sample_wrap .tbl td input {border:1px solid #d1d1d1; padding-left:5px; font-size:11px;}
        #sample_wrap .tbl td input.w300 {width:300px;}
        #sample_wrap .tbl td input.w200 {width:200px;}
        #sample_wrap .tbl td input.w100 {width:100px;}
        #sample_wrap .btnset {text-align:center; margin-bottom:20px;}
        #sample_wrap .btnset a.submit {background:#e44541;}
        #sample_wrap .btnset a.submit:hover {background:#bd2a27;}
        #sample_wrap .btnset a.home {background:#4383b0;}
        #sample_wrap .btnset a.home:hover {background:#245f89;}
        #sample_wrap .btnset a {display:inline-block; font-size:11px; letter-spacing:1px; font-weight:bold; color:#fff; width:70px; padding:10px 0; margin:0 5px;}
        #sample_wrap .btnset input.submit {background:#e44541; border:0px; font-size:11px; letter-spacing:1px; font-weight:bold; color:#fff; width:70px; padding:10px 0; margin:0 5px;}
        #sample_wrap .footer {width:500px; margin:10px auto 0; font-size:11px; text-align:center;}

        p.txt {padding:15px; line-height:16px;  color:#4383b0; white-space: pre; line-height: 12px;}
        p.txt span {font-weight:bold; color:#e44541;}
    </style>
    <script type="text/javascript">
    </script>
</head>

<body>
<div id="sample_wrap">

    <!-- 타이틀 Start -->
    <h1>[결과출력] <span>이 페이지는 요청 결과를 출력하는 샘플(예시) 페이지입니다.</span></h1>
    <!-- 타이틀 End -->

    <div class="sample">

        <!--상단 테이블 Start-->
        <p>
            요청 결과를 출력하는 페이지입니다.<br />
            요청이 정상적으로 처리된 경우 결과코드(res_cd)값이 0000으로 표시됩니다.
        </p>
        <!--상단 테이블 End-->
        <%
            /* ============================================================================== */
    /* =   처리 결과 코드 및 메시지 출력(결과페이지에 반드시 출력해주시기 바랍니다.)= */
    /* = -------------------------------------------------------------------------- = */
    /* =   처리 정상 : res_cd값이 0000으로 설정됩니다.                              = */
    /* =   처리 실패 : res_cd값이 0000이외의 값으로 설정됩니다.                     = */
    /* = -------------------------------------------------------------------------- = */
        %>
        <h2>&sdot; 결제 결과</h2>
        <table class="tbl" cellpadding="0" cellspacing="0">
            <!-- 결과 코드 -->
            <tr>
                <th>결과 코드</th>
                <td><%=res_cd%></td>
            </tr>
            <!-- 결과 메시지 -->
            <tr>
                <th>결과 메세지</th>
                <td><%=res_msg%></td>
            </tr>
        </table>
        <!-- 매입 요청/처음으로 이미지 버튼 -->
        <div class="btnset">
            <a href="/order/refundTest" class="home">처음으로</a>
        </div>
    </div>
    <div class="footer">
        Copyright (c) KCP INC. All Rights reserved.
    </div>
</div>
</body>
</html>