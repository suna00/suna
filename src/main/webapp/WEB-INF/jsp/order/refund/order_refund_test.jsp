<%@ page language="java" contentType="text/html;charset=euc-kr"%>
<%
    /* ============================================================================== */
    /* =   PAGE : ȯ�� ��� PAGE                                                    = */
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
        #sample_index a {position:relative; font-family:HY�߰��; display:inline-block; color:#fff;  font-size:16px; width:270px; text-align:leftr; padding:25px 0 20px 20px; margin-bottom:20px; cursor: pointer;
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
                alert( "KCP �ŷ� ��ȣ 14�ڸ��� �Է��ϼ���" );
                form.tno.focus();
                form.tno.select();
                return false;
            }
            else if ( form.mod_bankcode.value == "bank_code_not_sel" )
            {
                alert( "ȯ�� �����ڵ带 �����ϼ���" );
                form.mod_bankcode.focus();
                return false;
            }
            else if ( form.mod_account.value == "" )
            {
                alert( "ȯ�ҹ����� ���¹�ȣ�� �Է��ϼ���" );
                form.mod_account.focus();
                form.mod_account.select();
                return false;
            }
            else if ( form.mod_depositor.value == "" )
            {
                alert( "ȯ�ҹ����� �����ָ��� �Է��ϼ���" );
                form.mod_depositor.focus();
                form.mod_depositor.select();
                return false;
            }
            else if ( form.mod_comp_type.value == "MDCP02" )
            {
                if ( form.mod_socno.value == "" )
                {
                    alert( "�Ǹ������� ���� �ֹι�ȣ�� �Է��ϼ���" );
                    form.mod_socno.focus();
                    form.mod_socno.select();
                    return false;
                }
                else if ( form.mod_socname.value == "" )
                {
                    alert( "�Ǹ������� ���� ������ �Է��ϼ���" );
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

        <!-- Ÿ��Ʋ Start -->
        <h1>[������] <span>�� �������� ���� ����� ����ϴ� ����(����) �������Դϴ�.</span></h1>
        <!-- Ÿ��Ʋ End -->
        <div class="sample">
            <!--��� ���̺� Start-->
            <p>
                ���� ����� ����ϴ� �������Դϴ�.<br />
                ��û�� ���������� ó���� ��� ����ڵ�(res_cd)���� 0000���� ǥ�õ˴ϴ�.
            </p>
            <!--��� ���̺� End-->

            <!-- �������� ���� -->
            <h2>&sdot; ȯ������</h2>
            <table class="tbl" cellpadding="0" cellspacing="0">
                <!-- ȯ�ҹ��� �����ڵ� -->
                <tr>
                    <th>ȯ�ҹ��� �����ڵ�</th>
                    <td>
                        <select name='mod_bankcode' class="frmselect">
                            <option value="bank_code_not_sel" selected>����</option>
                            <option value="BK89">���̹�ũ</option>
                            <option value="BK90">īī����ũ</option>
                            <option value="BK39">�泲����</option>
                            <option value="BK34">��������</option>
                            <option value="BK04">��������</option>
                            <option value="BK03">�������</option>
                            <option value="BK11">����</option>
                            <option value="BK31">�뱸����</option>
                            <option value="BK32">�λ�����</option>
                            <option value="BK45">�������ݰ�</option>
                            <option value="BK07">����</option>
                            <option value="BK88">��������</option>
                            <option value="BK48">����</option>
                            <option value="BK20">�츮����</option>
                            <option value="BK71">��ü��</option>
                            <option value="BK35">��������</option>
                            <option value="BK81">KEB�ϳ�����</option>
                            <option value="BK27">�ѱ���Ƽ����</option>
                            <option value="BK54">HSBC</option>
                            <option value="BK23">SC��������</option>
                            <option value="BK02">�������</option>
                            <option value="BK37">��������</option>
                            <option value="B209">��������</option>
                            <option value="B218">��������</option>
                            <option value="B230">�̷���������</option>
                            <option value="B243">�ѱ���������</option>
                            <option value="B247">�츮��������</option>
                            <option value="B262">������������</option>
                            <option value="B263">HMC��������</option>
                            <option value="B266">SK����</option>
                            <option value="B267">�������</option>
                            <option value="B270">�ϳ���������</option>
                            <option value="B278">���ѱ�������</option>
                            <option value="B279">��������</option>
                            <option value="B280">������������</option>
                            <option value="B287">�޸���</option>
                            <option value="B291">�ſ�����</option>
                            <option value="B240">�Ｚ����</option>
                            <option value="B269">��ȭ����</option>
                            <option value="B238">�������</option>
                        </select>
                    </td>
                </tr>
                <!-- ȯ�ҹ��� ���� -->
                <tr>
                    <th>ȯ�ҹ��� ����</th>
                    <td><input type="text" name="mod_account" value=""  class="frminput" size="20" maxlength="20"/></td>
                </tr>
                <!-- ȯ�ҹ��� �����ָ� -->
                <tr>
                    <th>ȯ�ҹ��� �����ָ�</th>
                    <td><input type="text" name="mod_depositor" value="" class="frminput" size="20" maxlength="50"/></td>
                </tr>
            </table>

            <!-- �Ǹ����� �Է� ���̺� Start -->
            <table id="show_sub_cert" style="display:none" class="tbl" cellpadding="0" cellspacing="0">
                <!-- ��û ���� : �������� ȯ�ҿ�û ���� -->
                <tr>
                    <th>���� ����</th>
                    <td>�Ǹ�+��������</td>
                </tr>
                <!-- Input : �ֹι�ȣ -->
                <tr>
                    <th>�ֹι�ȣ</th>
                    <td><input type="text" name="mod_socno" value=""  class="frminput" size="20" maxlength="13"/></td>
                </tr>
                <!-- Input : ���� -->
                <tr>
                    <th>����</th>
                    <td><input type="text" name="mod_socname" value="" class="frminput" size="20" maxlength="10"/></td>
                </tr>
            </table>

            <!-- ȯ�� ��� ��û -->
            <h2>&sdot; ȯ�� ��� ��û</h2>
            <table class="tbl" cellpadding="0" cellspacing="0">

                <!-- �������� -->
                <tr>
                    <th>���� ����</th>
                    <td>
                        <select name="mod_comp_type" class="frmselect" onChange="sub_cert_chk(this.value);"/>
                        <option value="MDCP01">��������+ȯ�ҵ��</option>
                        <option value="MDCP02">(�Ǹ�+����)����+ȯ�ҵ��</option>
                        </select>
                    </td>
                </tr>
                <!-- �ŷ���ȣ -->
                <tr>
                    <th>�ŷ���ȣ</th>
                    <td><input type="text" name="tno" value=""  class="frminput" size="20" maxlength="14"/></td>
                </tr>
                <!-- �������� -->
                <tr>
                    <th>���� ����</th>
                    <td>
                        <select name="mod_type" class="frmselect" onChange="sub_cancel_chk(this.value);"/>
                        <option value="STHD">��ü ȯ�ҿ�û</option>
                        <option value="STPD">�κ� ȯ�ҿ�û</option>
                        </select>
                    </td>
                </tr>
                <!-- ������� -->
                <tr>
                    <th>�������</th>
                    <td><input type="text" name="mod_desc" value=""  class="frminput" size="40" maxlength="100"/></td>
                </tr>
            </table>

            <!-- ���� ȯ�� ��û ���� �Է� ���̺� Start -->
            <table id="show_sub_cancel" style="display:none" class="tbl" cellpadding="0" cellspacing="0">
                <!-- ��û ���� : �������� ȯ�ҿ�û ���� -->
                <tr>
                    <th>��û ����</th>
                    <td>�κ� ȯ�ҿ�û</td>
                </tr>
                <!-- Input : �κ�ȯ�� ��û �ݾ� �Է� -->
                <tr>
                    <th>�κ�ȯ�� ��û �ݾ�</th>
                    <td><input type="text" name="mod_mny" value=""  class="frminput" size="20" maxlength="10"/></td>
                </tr>
                <!-- Input : �κ�ȯ�� �� ���� �ݾ�(rem_mny) �Է� -->
                <tr>
                    <th>�κ�ȯ�� �� ���� �ݾ�</th>
                    <td><input type="text" name="rem_mny" value="" class="frminput" size="20" maxlength="10"/></td>
                </tr>
            </table>

            <!-- ���� ��ư ���̺� Start -->
            <div class="btnset">
                <table align="center" cellspacing="0" cellpadding="0" class="margin_top_20">
                    <tr id="show_pay_btn">
                        <td colspan="2" align="center">
                            <input name="" type="submit" class="submit" value="ȯ�ҿ�û" onclick="return jsf__go_mod(this.form);"/>
                            <a href="index.html" class="home">ó������</a>
            </div>
            </td>
            </tr>
            <!-- ���� ���� ���Դϴ�. �޽��� -->
            <tr id="show_progress" style="display:none">
                <td colspan="2" class="center red" >ȯ�� ���� ���Դϴ�. ��ø� ��ٷ� �ֽʽÿ�...</td>
            </tr>
            </table>
        </div>
        <!-- ���� ��ư ���̺� End -->
</div>
<!-- ���� ��ư ���̺� End -->
<div class="footer">
    Copyright (c) KCP INC. All Rights reserved.
</div>
<input type="hidden" name = "req_tx"        value="mod"/>
</form>
</div>
</body>
</html>