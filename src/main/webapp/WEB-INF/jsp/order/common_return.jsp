<%@ page import="net.ion.ice.ApplicationContextManager" %>
<%@ page import="net.ion.ice.core.data.bind.NodeBindingService" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="net.ion.ice.service.OrderService" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.apache.log4j.Logger" %>
<%@ page language="java" contentType="text/html;charset=utf-8"%>
<%
    /* ============================================================================== */
    /* =   PAGE : 공통 통보 PAGE                                                    = */
    /* = -------------------------------------------------------------------------- = */
    /* =   연동시 오류가 발생하는 경우 아래의 주소로 접속하셔서 확인하시기 바랍니다.= */
    /* =   접속 주소 : http://kcp.co.kr/technique.requestcode.do                    = */
    /* = -------------------------------------------------------------------------- = */
    /* =   Copyright (c)  2016   NHN KCP Inc.   All Rights Reserverd.               = */
    /* ============================================================================== */
%>
<%!
    static Logger logger = Logger.getLogger("common_return.jsp");
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
<% request.setCharacterEncoding("utf-8"); %>
<%
    /* ============================================================================== */
    /* =   01. 공통 통보 페이지 설명(필독!!)                                        = */
    /* = -------------------------------------------------------------------------- = */
    /* =   공통 통보 페이지에서는,                                                  = */
    /* =   가상계좌 입금 통보 데이터를 KCP를 통해 실시간으로 통보 받을 수 있습니다. = */
    /* =                                                                            = */
    /* =   common_return 페이지는 이러한 통보 데이터를 받기 위한 샘플 페이지        = */
    /* =   입니다. 현재의 페이지를 업체에 맞게 수정하신 후, 아래 사항을 참고하셔서  = */
    /* =   KCP 관리자 페이지에 등록해 주시기 바랍니다.                              = */
    /* =                                                                            = */
    /* =   등록 방법은 다음과 같습니다.                                             = */
    /* =  - KCP 관리자페이지(admin.kcp.co.kr)에 로그인 합니다.                      = */
    /* =  - [쇼핑몰 관리] -> [정보변경] -> [공통 URL 정보] -> 공통 URL 변경 후에    = */
    /* =    가맹점 URL을 입력합니다.                                                = */
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   02. 공통 통보 데이터 받기                                                = */
    /* = -------------------------------------------------------------------------- = */
    String site_cd      = f_get_parm( request.getParameter( "site_cd"      ) );  // 사이트 코드
    String tno          = f_get_parm( request.getParameter( "tno"          ) );  // KCP 거래번호
    String order_no     = f_get_parm( request.getParameter( "order_no"     ) );  // 주문번호
    String tx_cd        = f_get_parm( request.getParameter( "tx_cd"        ) );  // 업무처리 구분 코드
    String tx_tm        = f_get_parm( request.getParameter( "tx_tm"        ) );  // 업무처리 완료 시간
    /* = -------------------------------------------------------------------------- = */
    String ipgm_name    = f_get_parm( request.getParameter( "ipgm_name" ) );     // 주문자명
    String remitter     = f_get_parm( request.getParameter( "remitter"  ) );     // 입금자명
    String ipgm_mnyx    = f_get_parm( request.getParameter( "ipgm_mnyx" ) );     // 입금 금액
    String bank_code    = f_get_parm( request.getParameter( "bank_code" ) );     // 은행코드
    String account      = f_get_parm( request.getParameter( "account"   ) );     // 가상계좌 입금계좌번호
    String op_cd        = f_get_parm( request.getParameter( "op_cd"     ) );     // 처리구분 코드
    String noti_id      = f_get_parm( request.getParameter( "noti_id"   ) );     // 통보 아이디
    String cash_a_no    = f_get_parm( request.getParameter( "cash_a_no" ) );     // 현금영수증 승인번호
    String cash_a_dt    = f_get_parm( request.getParameter( "cash_a_dt" ) );     // 현금영수증 승인시간
    String cash_no      = f_get_parm( request.getParameter( "cash_no"   ) );     // 현금영수증 거래번호
    /* = -------------------------------------------------------------------------- = */
    OrderService orderService = (OrderService) ApplicationContextManager.getContext().getBean("orderService");
    Map<String, Object> storeOrderSheetMap = new HashMap<>();
    Map<String, Object> storeCashReceiptMap = new HashMap<>();

    storeOrderSheetMap.put("accSiteCd", site_cd);           //사이트코드
    storeOrderSheetMap.put("accTno", tno);                  //KCP거래번호
    storeOrderSheetMap.put("orderSheetId", order_no);       //주문번호
    storeOrderSheetMap.put("accTxCd", tx_cd);               //업무처리구분코드
    storeOrderSheetMap.put("accTxTm", tx_tm);               //업무처리완료시간
    storeOrderSheetMap.put("accIpgmName", ipgm_name);       //주문자명
    storeOrderSheetMap.put("accRemitter", remitter);        //입금자명
    storeOrderSheetMap.put("accIpgmMnyx", ipgm_mnyx);       //입금금액
    storeOrderSheetMap.put("accBankCode", bank_code);       //은행코드
    storeOrderSheetMap.put("accAccount", account);          //가상계좌입금계좌번호
    storeOrderSheetMap.put("accOpCd", op_cd);               //처리구분코드
    storeOrderSheetMap.put("accNotiId", noti_id);           //통보아이디
    storeOrderSheetMap.put("accCashAno", cash_a_no);        //현금영수증승인번호
    storeOrderSheetMap.put("accCashAdt", cash_a_dt);        //현금영수증승인시간
    storeOrderSheetMap.put("accCashNo", cash_no);           //현금영수증거래번호

    storeCashReceiptMap.put("cashNo", cash_no);             //현금영수증거래번호
    storeCashReceiptMap.put("receiptNo",cash_a_no);         //현금영수증승인번호
    storeCashReceiptMap.put("appTime", tx_tm);              //업무처리완료시간
    storeCashReceiptMap.put("regStat", "NTNC");             //등록상태
    storeCashReceiptMap.put("remMny", "국세청 등록완료");        //상태설명
    storeCashReceiptMap.put("orderSheetId", order_no);      //주문번호


    orderService.createCashReceipt(storeCashReceiptMap);          //현금영수증 저장
    orderService.accountTransferUpdate(storeOrderSheetMap);//주문서관련업데이트

    /* = -------------------------------------------------------------------------- = */
    /* =   02-1. 가상계좌 입금 통보 데이터 받기                                     = */
    /* = -------------------------------------------------------------------------- = */
    if ( tx_cd.equals("TX00") )
    {
        ipgm_name = f_get_parm( request.getParameter( "ipgm_name" ) );           // 주문자명
        remitter  = f_get_parm( request.getParameter( "remitter"  ) );           // 입금자명
        ipgm_mnyx = f_get_parm( request.getParameter( "ipgm_mnyx" ) );           // 입금 금액
        bank_code = f_get_parm( request.getParameter( "bank_code" ) );           // 은행코드
        account   = f_get_parm( request.getParameter( "account"   ) );           // 가상계좌 입금계좌번호
        op_cd     = f_get_parm( request.getParameter( "op_cd"     ) );           // 처리구분 코드
        noti_id   = f_get_parm( request.getParameter( "noti_id"   ) );           // 통보 아이디
        cash_a_no = f_get_parm( request.getParameter( "cash_a_no" ) );           // 현금영수증 승인번호
        cash_a_dt = f_get_parm( request.getParameter( "cash_a_dt" ) );           // 현금영수증 승인시간
        cash_no   = f_get_parm( request.getParameter( "cash_no"   ) );           // 현금영수증 거래번호
    }


    /* ============================================================================== */
    /* =   03. 공통 통보 결과를 업체 자체적으로 DB 처리 작업하시는 부분입니다.      = */
    /* = -------------------------------------------------------------------------- = */
    /* =   통보 결과를 DB 작업 하는 과정에서 정상적으로 통보된 건에 대해 DB 작업에  = */
    /* =   실패하여 DB update 가 완료되지 않은 경우, 결과를 재통보 받을 수 있는     = */
    /* =   프로세스가 구성되어 있습니다.                                            = */
    /* =                                                                            = */
    /* =   * DB update가 정상적으로 완료된 경우                                     = */
    /* =   하단의 [04. result 값 세팅 하기] 에서 result 값의 value값을 0000으로     = */
    /* =   설정해 주시기 바랍니다.                                                  = */
    /* =                                                                            = */
    /* =   * DB update가 실패한 경우                                                = */
    /* =   하단의 [04. result 값 세팅 하기] 에서 result 값의 value값을 0000이외의   = */
    /* =   값으로 설정해 주시기 바랍니다.                                           = */
    /* = -------------------------------------------------------------------------- = */

    /* = -------------------------------------------------------------------------- = */
    /* =   03-1. 가상계좌 입금 통보 데이터 DB 처리 작업 부분                        = */
    /* = -------------------------------------------------------------------------- = */
    if ( tx_cd.equals("TX00") )
    {

    }

    /* = -------------------------------------------------------------------------- = */
    /* =   03-2. 모바일계좌이체 통보 데이터 DB 처리 작업 부분                       = */
    /* = -------------------------------------------------------------------------- = */
    else if ( tx_cd.equals("TX08") )
    {
    }
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   04. result 값 세팅 하기                                                  = */
    /* = -------------------------------------------------------------------------- = */
    /* =   정상적으로 처리된 경우 value값을 0000으로 설정하여 주시기 바랍니다.      = */
    /* ============================================================================== */

    /* ============================================================================== */
    /* =   로그                                                  = */
    /* ============================================================================== */

    logger.info("=============common_return.jsp(" +order_no+ ")=============");
    logger.info("사이트 코드(site_cd) : "+ site_cd);
    logger.info("KCP 거래번호(tno) : "+ tno);
    logger.info("주문번호(order_no) : "+ order_no);
    logger.info("업무처리 구분 코드(tx_cd) : "+ tx_cd);
    logger.info("업무처리 완료 시간(tx_tm) : "+ tx_tm);
    logger.info("주문자명(ipgm_name) : "+ ipgm_name);
    logger.info("입금자명(remitter) : "+ remitter);
    logger.info("입금 금액(ipgm_mnyx) : "+ ipgm_mnyx);
    logger.info("은행코드(bank_code) : "+ bank_code);
    logger.info("가상계좌 입금계좌번호(account) : "+ account);
    logger.info("처리구분 코드(op_cd) : "+ op_cd);
    logger.info("통보 아이디(noti_id) : "+ noti_id);
    logger.info("현금영수증 승인번호(cash_a_no) : "+ cash_a_no);
    logger.info("현금영수증 승인시간(cash_a_dt) : "+ cash_a_dt);
    logger.info("현금영수증 거래번호(cash_no) : "+ cash_no);
    logger.info("\"==========================================================\"");
%>
<html><body><form><input type="hidden" name="result" value="0000"></form></body></html>