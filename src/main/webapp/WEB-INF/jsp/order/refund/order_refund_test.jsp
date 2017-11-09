<%@ page language="java" contentType="text/html;charset=euc-kr"%>
<%
    /* ============================================================================== */
    /* =   PAGE : 환불 등록 PAGE                                                    = */
    /* = -------------------------------------------------------------------------- = */
    /* =   Copyright (c)  2013   KCP Inc.   All Rights Reserved.                    = */
    /* ============================================================================== */
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

        function  jsf__go_mod( form )
        {
            if ( form.tno.value.length < 14 )
            {
                alert( "KCP 거래 번호 14자리를 입력하세요" );
                form.tno.focus();
                form.tno.select();
                return false;
            }
            else if ( form.mod_bankcode.value == "bank_code_not_sel" )
            {
                alert( "환불 은행코드를 선택하세요" );
                form.mod_bankcode.focus();
                return false;
            }
            else if ( form.mod_account.value == "" )
            {
                alert( "환불받으실 계좌번호를 입력하세요" );
                form.mod_account.focus();
                form.mod_account.select();
                return false;
            }
            else if ( form.mod_depositor.value == "" )
            {
                alert( "환불받으실 계좌주명을 입력하세요" );
                form.mod_depositor.focus();
                form.mod_depositor.select();
                return false;
            }
            else if ( form.mod_comp_type.value == "MDCP02" )
            {
                if ( form.mod_socno.value == "" )
                {
                    alert( "실명인증을 위한 주민번호를 입력하세요" );
                    form.mod_socno.focus();
                    form.mod_socno.select();
                    return false;
                }
                else if ( form.mod_socname.value == "" )
                {
                    alert( "실명인증을 위한 성명을 입력하세요" );
                    form.mod_socname.focus();
                    form.mod_socname.select();
                    return false;
                }
            }
            else
            {
                return true;
            }
        }

        function  sub_cancel_chk( val )
        {
            if( val == "STPD")
            {
                show_sub_cancel.style.display = "block";
            }
            else
            {
                show_sub_cancel.style.display = "none";
            }
        }

        function sub_cert_chk( val )
        {
            if( val == "MDCP02" )
            {
                show_sub_cert.style.display = "block";
            }
            else
            {
                show_sub_cert.style.display = "none";
            }
        }
    </script>
</head>

<body oncontextmenu="return false;" ondragstart="return false;" onselectstart="return false;">

<div id="sample_wrap">

    <form name="acnt_form" action="/order/refundTest1" method="post">

        <!-- 타이틀 Start -->
        <h1>[결과출력] <span>이 페이지는 결제 결과를 출력하는 샘플(예시) 페이지입니다.</span></h1>
        <!-- 타이틀 End -->
        <div class="sample">
            <!--상단 테이블 Start-->
            <p>
                결제 결과를 출력하는 페이지입니다.<br />
                요청이 정상적으로 처리된 경우 결과코드(res_cd)값이 0000으로 표시됩니다.
            </p>
            <!--상단 테이블 End-->

            <!-- 계좌인증 정보 -->
            <h2>&sdot; 환불정보</h2>
            <table class="tbl" cellpadding="0" cellspacing="0">
                <!-- 환불받을 은행코드 -->
                <tr>
                    <th>환불받을 은행코드</th>
                    <td>
                        <select name='mod_bankcode' class="frmselect">
                            <option value="bank_code_not_sel" selected>선택</option>
                            <option value="BK89">케이뱅크</option>
                            <option value="BK90">카카오뱅크</option>
                            <option value="BK39">경남은행</option>
                            <option value="BK34">광주은행</option>
                            <option value="BK04">국민은행</option>
                            <option value="BK03">기업은행</option>
                            <option value="BK11">농협</option>
                            <option value="BK31">대구은행</option>
                            <option value="BK32">부산은행</option>
                            <option value="BK45">새마을금고</option>
                            <option value="BK07">수협</option>
                            <option value="BK88">신한은행</option>
                            <option value="BK48">신협</option>
                            <option value="BK20">우리은행</option>
                            <option value="BK71">우체국</option>
                            <option value="BK35">제주은행</option>
                            <option value="BK81">KEB하나은행</option>
                            <option value="BK27">한국시티은행</option>
                            <option value="BK54">HSBC</option>
                            <option value="BK23">SC제일은행</option>
                            <option value="BK02">산업은행</option>
                            <option value="BK37">전북은행</option>
                            <option value="B209">동양증권</option>
                            <option value="B218">현대증권</option>
                            <option value="B230">미래에셋증권</option>
                            <option value="B243">한국투자증권</option>
                            <option value="B247">우리투자증권</option>
                            <option value="B262">하이투자증권</option>
                            <option value="B263">HMC투자증권</option>
                            <option value="B266">SK증권</option>
                            <option value="B267">대신증권</option>
                            <option value="B270">하나대투증권</option>
                            <option value="B278">신한금융투자</option>
                            <option value="B279">동부증권</option>
                            <option value="B280">유진투자증권</option>
                            <option value="B287">메리츠</option>
                            <option value="B291">신영증권</option>
                            <option value="B240">삼성증권</option>
                            <option value="B269">한화증권</option>
                            <option value="B238">대우증권</option>
                        </select>
                    </td>
                </tr>
                <!-- 환불받을 계좌 -->
                <tr>
                    <th>환불받을 계좌</th>
                    <td><input type="text" name="mod_account" value=""  class="frminput" size="20" maxlength="20"/></td>
                </tr>
                <!-- 환불받을 계좌주명 -->
                <tr>
                    <th>환불받을 계좌주명</th>
                    <td><input type="text" name="mod_depositor" value="" class="frminput" size="20" maxlength="50"/></td>
                </tr>
            </table>

            <!-- 실명인증 입력 테이블 Start -->
            <table id="show_sub_cert" style="display:none" class="tbl" cellpadding="0" cellspacing="0">
                <!-- 요청 구분 : 개별승인 환불요청 정보 -->
                <tr>
                    <th>인증 구분</th>
                    <td>실명+계좌인증</td>
                </tr>
                <!-- Input : 주민번호 -->
                <tr>
                    <th>주민번호</th>
                    <td><input type="text" name="mod_socno" value=""  class="frminput" size="20" maxlength="13"/></td>
                </tr>
                <!-- Input : 성명 -->
                <tr>
                    <th>성명</th>
                    <td><input type="text" name="mod_socname" value="" class="frminput" size="20" maxlength="10"/></td>
                </tr>
            </table>

            <!-- 환불 등록 요청 -->
            <h2>&sdot; 환불 등록 요청</h2>
            <table class="tbl" cellpadding="0" cellspacing="0">

                <!-- 변경유형 -->
                <tr>
                    <th>변경 유형</th>
                    <td>
                        <select name="mod_comp_type" class="frmselect" onChange="sub_cert_chk(this.value);"/>
                        <option value="MDCP01">계좌인증+환불등록</option>
                        <option value="MDCP02">(실명+계좌)인증+환불등록</option>
                        </select>
                    </td>
                </tr>
                <!-- 거래번호 -->
                <tr>
                    <th>거래번호</th>
                    <td><input type="text" name="tno" value=""  class="frminput" size="20" maxlength="14"/></td>
                </tr>
                <!-- 변경유형 -->
                <tr>
                    <th>변경 유형</th>
                    <td>
                        <select name="mod_type" class="frmselect" onChange="sub_cancel_chk(this.value);"/>
                        <option value="STHD">전체 환불요청</option>
                        <option value="STPD">부분 환불요청</option>
                        </select>
                    </td>
                </tr>
                <!-- 변경사유 -->
                <tr>
                    <th>변경사유</th>
                    <td><input type="text" name="mod_desc" value=""  class="frminput" size="40" maxlength="100"/></td>
                </tr>
            </table>

            <!-- 개별 환불 요청 정보 입력 테이블 Start -->
            <table id="show_sub_cancel" style="display:none" class="tbl" cellpadding="0" cellspacing="0">
                <!-- 요청 구분 : 개별승인 환불요청 정보 -->
                <tr>
                    <th>요청 구분</th>
                    <td>부분 환불요청</td>
                </tr>
                <!-- Input : 부분환불 요청 금액 입력 -->
                <tr>
                    <th>부분환불 요청 금액</th>
                    <td><input type="text" name="mod_mny" value=""  class="frminput" size="20" maxlength="10"/></td>
                </tr>
                <!-- Input : 부분환불 전 남은 금액(rem_mny) 입력 -->
                <tr>
                    <th>부분환불 전 남은 금액</th>
                    <td><input type="text" name="rem_mny" value="" class="frminput" size="20" maxlength="10"/></td>
                </tr>
            </table>

            <!-- 결제 버튼 테이블 Start -->
            <div class="btnset">
                <table align="center" cellspacing="0" cellpadding="0" class="margin_top_20">
                    <tr id="show_pay_btn">
                        <td colspan="2" align="center">
                            <input name="" type="submit" class="submit" value="환불요청" onclick="return jsf__go_mod(this.form);"/>
                            <a href="index.html" class="home">처음으로</a>
            </div>
            </td>
            </tr>
            <!-- 결제 진행 중입니다. 메시지 -->
            <tr id="show_progress" style="display:none">
                <td colspan="2" class="center red" >환불 진행 중입니다. 잠시만 기다려 주십시오...</td>
            </tr>
            </table>
        </div>
        <!-- 결제 버튼 테이블 End -->
</div>
<!-- 결제 버튼 테이블 End -->
<div class="footer">
    Copyright (c) KCP INC. All Rights reserved.
</div>
<input type="hidden" name = "req_tx"        value="mod"/>
</form>
</div>
</body>
</html>