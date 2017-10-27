<%--
  Created by IntelliJ IDEA.
  User: yoonseonwoong
  Date: 2017. 9. 9.
  Time: PM 7:57
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java" contentType="text/html;charset=utf-8"%>

<%
    request.setCharacterEncoding ( "utf-8" ) ;
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" >
<head>
    <title>*** NHN KCP [AX-HUB Version] ***</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link href="/css/style.css" rel="stylesheet" type="text/css" id="cssLink"/>

    <script type="text/javascript">
        function receiptView3()
        {
            var receiptWin3 = "http://devadmin.kcp.co.kr/Modules/Noti/TEST_Vcnt_Noti.jsp";
            window.open(receiptWin3, "", "width=520, height=300");
        }
    </script>
</head>

<body>

<div id="sample_wrap">
    <table class="tbl" cellpadding="0" cellspacing="0">
        <!-- 결제수단 : 가상계좌 -->
        <tr>
            <th>가상계좌 모의입금</br>(테스트시 사용)</th>
            <td class="sub_content1"><a href="javascript:receiptView3()"><img src="./img/btn_vcn.png" alt="모의입금 페이지로 이동합니다." /></a>
        </tr>
    </table>

</div>
</body>
</html>
