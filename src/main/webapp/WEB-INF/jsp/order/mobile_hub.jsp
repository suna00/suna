<%@ page language="java" contentType="text/html;charset=UTF-8" %>

<%
    /* ============================================================================== */
    /* =   PAGE : 지불 요청 및 결과 처리 PAGE                                       = */
    /* = -------------------------------------------------------------------------- = */
    /* =   연동시 오류가 발생하는 경우 아래의 주소로 접속하셔서 확인하시기 바랍니다.= */
    /* =   접속 주소 : http://kcp.co.kr/technique.requestcode.do                    = */
    /* = -------------------------------------------------------------------------- = */
    /* =   Copyright (c)  2016  NHN KCP Inc.   All Rights Reserverd.                = */
    /* ============================================================================== */

    /* ============================================================================== */
    /* =   환경 설정 파일 Include                                                   = */
    /* = -------------------------------------------------------------------------- = */
    /* =   ※ 필수                                                                  = */
    /* =   테스트 및 실결제 연동시 site_conf_inc.jsp파일을 수정하시기 바랍니다.     = */
    /* = -------------------------------------------------------------------------- = */
%>
<%@ page import="com.kcp.J_PP_CLI_N" %>
<%@ page import="net.ion.ice.ApplicationContextManager" %>
<%@ page import="net.ion.ice.service.OrderService" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.concurrent.ConcurrentHashMap" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.apache.log4j.Logger" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="net.ion.ice.core.json.JsonUtils" %>
<%@ include file="cfg/site_conf_inc.jsp" %>
<%
    /* = -------------------------------------------------------------------------- = */
    /* =   환경 설정 파일 Include END                                               = */
    /* ============================================================================== */

%>

<%!
    /* ============================================================================== */
    /* =   null 값을 처리하는 메소드                                                = */
    /* = -------------------------------------------------------------------------- = */
    public String f_get_parm(String val) {
        if (val == null) val = "";
        return val;
    }
    /* ============================================================================== */
%>

<%!
    static Logger logger = Logger.getLogger("mobile_hub.jsp");

%>

<%
    /* ============================================================================== */
    /* =   POST 형식 체크부분                                                       = */
    /* = -------------------------------------------------------------------------- = */
    if (!request.getMethod().equals("POST")) {
        out.println("잘못된 경로로 접속하였습니다.");
        return;
    }
    /* ============================================================================== */
%>
<%
    OrderService orderService = (OrderService) ApplicationContextManager.getContext().getBean("orderService");

    request.setCharacterEncoding("utf-8");

    Map<String, Object> resultData = JsonUtils.parsingJsonToMap(request.getParameter("result_data"));
    logger.info("resultData: " + resultData);
    /* ============================================================================== */
    /* =   02. 지불 요청 정보 설정                                                  = */
    /* = -------------------------------------------------------------------------- = */
    String req_tx = JsonUtils.getStringValue(resultData, "req_tx");                 // 요청 종류
    String tran_cd = JsonUtils.getStringValue(resultData, "tran_cd");               // 처리 종류
    /* = -------------------------------------------------------------------------- = */
    String cust_ip = f_get_parm(request.getRemoteAddr());                       // 요청 IP
    String ordr_idxx = JsonUtils.getStringValue(resultData, "ordr_idxx");           // 쇼핑몰 주문번호
    String good_name = JsonUtils.getStringValue(resultData, "good_name");           // 상품명
    /* = -------------------------------------------------------------------------- = */
    String res_cd = "";                                                         // 응답코드
    String res_msg = "";                                                        // 응답 메세지
    String tno = JsonUtils.getStringValue(resultData, "tno");                       // KCP 거래 고유 번호
    /* = -------------------------------------------------------------------------- = */
    String buyr_name = JsonUtils.getStringValue(resultData, "buyr_name");           // 주문자명
    String buyr_tel1 = JsonUtils.getStringValue(resultData, "buyr_tel1");           // 주문자 전화번호
    String buyr_tel2 = JsonUtils.getStringValue(resultData, "buyr_tel2");           // 주문자 핸드폰 번호
    String buyr_mail = JsonUtils.getStringValue(resultData, "buyr_mail");           // 주문자 E-mail 주소
    /* = -------------------------------------------------------------------------- = */
    String use_pay_method = JsonUtils.getStringValue(resultData, "use_pay_method"); // 결제 방법
    String bSucc = "false";                                                     // 업체 DB 처리 성공 여부
    /* = -------------------------------------------------------------------------- = */
    String app_time = "";                                                       // 승인시간 (모든 결제 수단 공통)
    String amount = "";                                                         // KCP 실제 거래금액
    String total_amount = "0";                                                  // 복합결제시 총 거래금액
    String coupon_mny = "";                                                     // 쿠폰금액
    /* = -------------------------------------------------------------------------- = */
    String card_cd = "";                                                        // 신용카드 코드
    String card_name = "";                                                      // 신용카드 명
    String app_no = "";                                                         // 신용카드 승인번호
    String noinf = "";                                                          // 신용카드 무이자 여부
    String quota = "";                                                          // 신용카드 할부개월
    String partcanc_yn = "";                                                    // 부분취소 가능유무
    String card_bin_type_01 = "";                                               // 카드구분1
    String card_bin_type_02 = "";                                               // 카드구분2
    String card_mny = "";                                                       // 카드결제금액
    /* = -------------------------------------------------------------------------- = */
    String bank_name = "";                                                      // 은행명
    String bank_code = "";                                                      // 은행코드
    String bk_mny = "";                                                         // 계좌이체결제금액
    /* = -------------------------------------------------------------------------- = */
    String bankname = "";                                                       // 입금 은행명
    String depositor = "";                                                      // 입금 계좌 예금주 성명
    String account = "";                                                        // 입금 계좌 번호
    String va_date = "";                                                        // 가상계좌 입금마감시간
    /* = ------------------------------------------------------------------------- = */
    String pnt_issue = "";                                                      // 결제 포인트사 코드
    String pnt_amount = "";                                                     // 적립금액 or 사용금액
    String pnt_app_time = "";                                                   // 승인시간
    String pnt_app_no = "";                                                     // 승인번호
    String add_pnt = "";                                                        // 발생 포인트
    String use_pnt = "";                                                        // 사용가능 포인트
    String rsv_pnt = "";                                                        // 총 누적 포인트
    /* = ------------------------------------------------------------------------- = */
    String commid = "";                                                         // 통신사코드
    String mobile_no = "";                                                      // 휴대폰번호
    /* = ------------------------------------------------------------------------- = */
    String shop_user_id = JsonUtils.getStringValue(resultData, "shop_user_id");     // 가맹점 고객 아이디
    String tk_van_code = "";                                                    // 발급사코드
    String tk_app_no = "";                                                      // 승인번호
    /* = -------------------------------------------------------------------------- = */
    String cash_yn = JsonUtils.getStringValue(resultData, "cash_yn"); // 현금 영수증 등록 여부
    String cash_authno = "";                                                 // 현금 영수증 승인 번호
    String cash_tr_code = JsonUtils.getStringValue(resultData, "cash_tr_code");  // 현금 영수증 발행 구분
    String cash_id_info = JsonUtils.getStringValue(resultData, "cash_id_info");  // 현금 영수증 등록 번호
    String cash_no = "";                                                     // 현금 영수증 거래 번호
    String good_mny = JsonUtils.getStringValue(resultData, "good_mny");

    String param_opt_1    = JsonUtils.getStringValue(resultData, "param_opt_1");
    String param_opt_2    = JsonUtils.getStringValue(resultData, "param_opt_2");

    logger.info("tran_cd: " + JsonUtils.getStringValue(resultData, "tran_cd"));
    logger.info("param_opt_1: " + param_opt_1);
    logger.info("param_opt_2: " + param_opt_2);

    /*쉬핑 데이터*/
    String shippingAddress = f_get_parm(request.getParameter("shippingAddress"));
    String shippingDetailedAddress = f_get_parm(request.getParameter("shippingDetailedAddress"));
    String shippingCellPhone = f_get_parm(request.getParameter("shippingCellPhone"));
    String shippingPhone = f_get_parm(request.getParameter("shippingPhone"));
    String deliveryMemo = f_get_parm(request.getParameter("deliveryMemo"));
    String addressName = f_get_parm(request.getParameter("addressName"));
    String postCode = f_get_parm(request.getParameter("postCode"));
    String recipient = f_get_parm(request.getParameter("recipient"));
    String deliveryType = f_get_parm(request.getParameter("deliveryType"));
    String addMyDeliveryAddress = f_get_parm(request.getParameter("addMyDeliveryAddress"));
    String changeDefaultAddress = f_get_parm(request.getParameter("changeDefaultAddress"));
    String myDeliveryAddressId = f_get_parm(request.getParameter("myDeliveryAddressId"));


    String useYPoint = request.getParameter("useYPoint");
    String useWelfarepoint = request.getParameter("useWelfarepoint");
    String usedCoupon = request.getParameter("usedCoupon");
    String memberNo = request.getParameter("memberNo");
    String siteId = request.getParameter("siteId");

    /* ============================================================================== */
    /* =   02. 지불 요청 정보 설정 END
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   03. 인스턴스 생성 및 초기화(변경 불가)                                   = */
    /* = -------------------------------------------------------------------------- = */
    /* =       결제에 필요한 인스턴스를 생성하고 초기화 합니다.                     = */
    /* = -------------------------------------------------------------------------- = */
    J_PP_CLI_N c_PayPlus = new J_PP_CLI_N();

    c_PayPlus.mf_init("", g_conf_gw_url, g_conf_gw_port, g_conf_tx_mode, g_conf_pay_log_dir);
    c_PayPlus.mf_init_set();

    /* ============================================================================== */
    /* =   03. 인스턴스 생성 및 초기화 END                                          = */
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   04. 처리 요청 정보 설정                                                  = */
    /* = -------------------------------------------------------------------------- = */
    /* = -------------------------------------------------------------------------- = */
    /* =   04-1. 승인 요청 정보 설정                                                = */
    /* = -------------------------------------------------------------------------- = */
    if (req_tx.equals("pay")) {
        c_PayPlus.mf_set_enc_data(JsonUtils.getStringValue(resultData, "enc_data"),
                JsonUtils.getStringValue(resultData, "enc_info"));

            /* 1 원은 실제로 업체에서 결제하셔야 될 원 금액을 넣어주셔야 합니다. 결제금액 유효성 검증 */

        int ordr_data_set_no;

        ordr_data_set_no = c_PayPlus.mf_add_set("ordr_data");
        Double orderPay = orderService.getFinalPrice(ordr_idxx, memberNo, useYPoint, useWelfarepoint, usedCoupon);
        c_PayPlus.mf_set_us(ordr_data_set_no, "ordr_mony", String.valueOf(orderPay.intValue()));


    }
    /* = -------------------------------------------------------------------------- = */
    /* =   04. 처리 요청 정보 설정 END                                              = */
    /* = ========================================================================== = */


    /* = ========================================================================== = */
    /* =   05. 실행                                                                 = */
    /* = -------------------------------------------------------------------------- = */
    if (tran_cd.length() > 0) {
        c_PayPlus.mf_do_tx(g_conf_site_cd, g_conf_site_key, tran_cd, "", ordr_idxx, g_conf_log_level, "1");
    } else {
        c_PayPlus.m_res_cd = "9562";
        c_PayPlus.m_res_msg = "연동 오류|tran_cd값이 설정되지 않았습니다.";
    }

    res_cd = c_PayPlus.m_res_cd;  // 결과 코드
    res_msg = c_PayPlus.m_res_msg; // 결과 메시지

    /* = -------------------------------------------------------------------------- = */
    /* =   05. 실행 END                                                             = */
    /* ============================================================================== */


    /* ============================================================================== */
    /* =   06. 승인 결과 값 추출                                                    = */
    /* = -------------------------------------------------------------------------- = */
    if (req_tx.equals("pay")) {
        if (res_cd.equals("0000")) {
            tno = c_PayPlus.mf_get_res("tno"); // KCP 거래 고유 번호
            amount = c_PayPlus.mf_get_res("amount"); // KCP 실제 거래 금액
            pnt_issue = c_PayPlus.mf_get_res("pnt_issue"); // 결제 포인트사 코드
            coupon_mny = c_PayPlus.mf_get_res("coupon_mny"); // 쿠폰금액

    /* = -------------------------------------------------------------------------- = */
    /* =   06-1. 신용카드 승인 결과 처리                                            = */
    /* = -------------------------------------------------------------------------- = */
            if (use_pay_method.equals("100000000000")) {
                card_cd = c_PayPlus.mf_get_res("card_cd"); // 카드사 코드
                card_name = c_PayPlus.mf_get_res("card_name"); // 카드사 명
                app_time = c_PayPlus.mf_get_res("app_time"); // 승인시간
                app_no = c_PayPlus.mf_get_res("app_no"); // 승인번호
                noinf = c_PayPlus.mf_get_res("noinf"); // 무이자 여부
                quota = c_PayPlus.mf_get_res("quota"); // 할부 개월 수
                partcanc_yn = c_PayPlus.mf_get_res("partcanc_yn"); // 부분취소 가능유무
                card_bin_type_01 = c_PayPlus.mf_get_res("card_bin_type_01"); // 카드구분1
                card_bin_type_02 = c_PayPlus.mf_get_res("card_bin_type_02"); // 카드구분2
                card_mny = c_PayPlus.mf_get_res("card_mny"); // 카드결제금액

                /* = -------------------------------------------------------------- = */
                /* =   06-1.1. 복합결제(포인트+신용카드) 승인 결과 처리             = */
                /* = -------------------------------------------------------------- = */
                if (pnt_issue.equals("SCSK") || pnt_issue.equals("SCWB")) {
                    pnt_amount = c_PayPlus.mf_get_res("pnt_amount"); // 적립금액 or 사용금액
                    pnt_app_time = c_PayPlus.mf_get_res("pnt_app_time"); // 승인시간
                    pnt_app_no = c_PayPlus.mf_get_res("pnt_app_no"); // 승인번호
                    add_pnt = c_PayPlus.mf_get_res("add_pnt"); // 발생 포인트
                    use_pnt = c_PayPlus.mf_get_res("use_pnt"); // 사용가능 포인트
                    rsv_pnt = c_PayPlus.mf_get_res("rsv_pnt"); // 총 누적 포인트
                    total_amount = amount + pnt_amount;                    // 복합결제시 총 거래금액
                }
            }

    /* = -------------------------------------------------------------------------- = */
    /* =   06-2. 계좌이체 승인 결과 처리                                            = */
    /* = -------------------------------------------------------------------------- = */
            if (use_pay_method.equals("010000000000")) {
                app_time = c_PayPlus.mf_get_res("app_time"); // 승인시간
                bank_name = c_PayPlus.mf_get_res("bank_name"); // 은행명
                bank_code = c_PayPlus.mf_get_res("bank_code"); // 은행코드n
                bk_mny = c_PayPlus.mf_get_res("bk_mny"); // 계좌이체결제금액
            }

    /* = -------------------------------------------------------------------------- = */
    /* =   06-3. 가상계좌 승인 결과 처리                                            = */
    /* = -------------------------------------------------------------------------- = */
            if (use_pay_method.equals("001000000000")) {
                bankname = c_PayPlus.mf_get_res("bankname"); // 입금할 은행 이름
                depositor = c_PayPlus.mf_get_res("depositor"); // 입금할 계좌 예금주
                account = c_PayPlus.mf_get_res("account"); // 입금할 계좌 번호
                va_date = c_PayPlus.mf_get_res("va_date"); // 가상계좌 입금마감시간
            }

    /* = -------------------------------------------------------------------------- = */
    /* =   06-4. 포인트 승인 결과 처리                                              = */
    /* = -------------------------------------------------------------------------- = */
            if (use_pay_method.equals("000100000000")) {
                pnt_amount = c_PayPlus.mf_get_res("pnt_amount"); // 적립금액 or 사용금액
                pnt_app_time = c_PayPlus.mf_get_res("pnt_app_time"); // 승인시간
                pnt_app_no = c_PayPlus.mf_get_res("pnt_app_no"); // 승인번호
                add_pnt = c_PayPlus.mf_get_res("add_pnt"); // 발생 포인트
                use_pnt = c_PayPlus.mf_get_res("use_pnt"); // 사용가능 포인트
                rsv_pnt = c_PayPlus.mf_get_res("rsv_pnt"); // 총 누적 포인트
            }

    /* = -------------------------------------------------------------------------- = */
    /* =   06-5. 휴대폰 승인 결과 처리                                              = */
    /* = -------------------------------------------------------------------------- = */
            if (use_pay_method.equals("000010000000")) {
                app_time = c_PayPlus.mf_get_res("hp_app_time"); // 승인 시간
                commid = c_PayPlus.mf_get_res("commid"); // 통신사 코드
                mobile_no = c_PayPlus.mf_get_res("mobile_no"); // 휴대폰 번호
            }

    /* = -------------------------------------------------------------------------- = */
    /* =   06-6. 상품권 승인 결과 처리                                              = */
    /* = -------------------------------------------------------------------------- = */
            if (use_pay_method.equals("000000001000")) {
                app_time = c_PayPlus.mf_get_res("tk_app_time"); // 승인 시간
                tk_van_code = c_PayPlus.mf_get_res("tk_van_code"); // 발급사 코드
                tk_app_no = c_PayPlus.mf_get_res("tk_app_no"); // 승인 번호
            }

    /* = -------------------------------------------------------------------------- = */
    /* =   06-7. 현금영수증 승인 결과 처리                                          = */
    /* = -------------------------------------------------------------------------- = */
            cash_authno = c_PayPlus.mf_get_res("cash_authno"); // 현금영수증 승인번호
            cash_no = c_PayPlus.mf_get_res("cash_no"); // 현금영수증 거래번호
        }
    }
    /* = -------------------------------------------------------------------------- = */
    /* =   06. 승인 결과 처리 END                                                   = */
    /* ============================================================================== */


    /* = ========================================================================== = */
    /* =   07. 승인 및 실패 결과 DB 처리                                            = */
    /* = -------------------------------------------------------------------------- = */
    /* =      결과를 업체 자체적으로 DB 처리 작업하시는 부분입니다.                 = */
    /* = -------------------------------------------------------------------------- = */

    Map<String, Object> responseMap = new ConcurrentHashMap<>();

    /*Payment*/
    responseMap.put("gConfSiteCd", g_conf_site_cd);
    responseMap.put("goodMny", good_mny);               //finalPrice
    responseMap.put("reqTx", req_tx);
    responseMap.put("usePayMethod", use_pay_method);
    responseMap.put("amount", amount);
    responseMap.put("totalAmount", total_amount);
    responseMap.put("cardMny", card_mny);
    responseMap.put("bkMny", bk_mny);
    responseMap.put("couponMny", coupon_mny);
    responseMap.put("resCd", res_cd);
    responseMap.put("resMsg", res_msg);
    responseMap.put("ordrIdxx", ordr_idxx);
    responseMap.put("orderSheetId", ordr_idxx);
    responseMap.put("tno", tno);
    responseMap.put("goodName", good_name);
    responseMap.put("buyrName", buyr_name);
    responseMap.put("buyrTel1", buyr_tel1);
    responseMap.put("buyrTel2", buyr_tel2);
    responseMap.put("buyrMail", buyr_mail);
    responseMap.put("appTime", app_time);
    responseMap.put("cardCd", card_cd);
    responseMap.put("cardName", card_name);
    responseMap.put("appNo", app_no);
    responseMap.put("noinf", noinf);
    responseMap.put("quota", quota);
    responseMap.put("partcancYn", partcanc_yn);
    responseMap.put("cardBinType01", card_bin_type_01);
    responseMap.put("cardBinType02", card_bin_type_02);
    responseMap.put("bankName", bank_name);
    responseMap.put("bankCode", bank_code);
    responseMap.put("depositBankName", bankname);
    responseMap.put("depositor", depositor);
    responseMap.put("account", account);
    responseMap.put("vaDate", va_date);
    responseMap.put("pntIssue", pnt_issue);
    responseMap.put("pntAppTime", pnt_app_time);
    responseMap.put("pntAppNo", pnt_app_no);
    responseMap.put("pntAmount", pnt_amount);
    responseMap.put("addPnt", add_pnt);
    responseMap.put("usePnt", use_pnt);
    responseMap.put("rsvPnt", rsv_pnt);
    responseMap.put("commid", commid);
    responseMap.put("mobileNo", mobile_no);
    responseMap.put("tkVanCode", tk_van_code);
    responseMap.put("tkAppNo", tk_app_no);
    responseMap.put("shopUserId", shop_user_id);
    responseMap.put("cashYn", cash_yn);
    responseMap.put("cashAuthno", cash_authno);
    responseMap.put("cashTrCode", cash_tr_code);
    responseMap.put("cashIdInfo", cash_id_info);
    responseMap.put("cashNo", cash_no);

    /*Delivery*/
    responseMap.put("addressName", addressName);
    responseMap.put("shippingAddress", shippingAddress);
    responseMap.put("shippingDetailedAddress", shippingDetailedAddress);
    responseMap.put("shippingCellPhone", shippingCellPhone);
    responseMap.put("shippingPhone", shippingPhone);
    responseMap.put("deliveryMemo", deliveryMemo);
    responseMap.put("postCode", postCode);
    responseMap.put("recipient", recipient);
    responseMap.put("deliveryType", deliveryType);
    responseMap.put("addMyDeliveryAddress", addMyDeliveryAddress);
    responseMap.put("changeDefaultAddress", changeDefaultAddress);
    responseMap.put("myDeliveryAddressId", myDeliveryAddressId);

    /*OrderSheet*/
    responseMap.put("useYPoint", useYPoint);
    responseMap.put("useWelfarepoint", useWelfarepoint);
    responseMap.put("usedCoupon", usedCoupon);
    responseMap.put("memberNo", memberNo);

    String pgId = orderService.createPgResponse(responseMap);

    if (req_tx.equals("pay")) {
    /* = -------------------------------------------------------------------------- = */
    /* =   07-1. 승인 결과 DB 처리(res_cd == "0000")                                = */
    /* = -------------------------------------------------------------------------- = */
    /* =        각 결제수단을 구분하시어 DB 처리를 하시기 바랍니다.                 = */
    /* = -------------------------------------------------------------------------- = */
        if (StringUtils.equals(res_cd, "0000")) {
            try {
                // 07-1-1. 신용카드
                if (use_pay_method.equals("100000000000")) {
                    responseMap.put("orderStatus", "order003"); //결제 완료
                    responseMap.put("usePayMethodName", "신용카드");
                    if (pnt_issue.equals("SCSK") || pnt_issue.equals("SCWB")) {
                        // 07-1-1-1. 복합결제(신용카드+포인트)
                        responseMap.put("orderStatus", "order003"); //결제 완료
                        responseMap.put("usePayMethodName", "신용카드+포인트");
                    }
                }

                // 07-1-2. 계좌이체
                if (use_pay_method.equals("010000000000")) {
                    responseMap.put("orderStatus", "order003"); //결제완료
                    responseMap.put("usePayMethodName", "계좌이체");
                }
                // 07-1-3. 가상계좌
                if (use_pay_method.equals("001000000000")) {
                    responseMap.put("orderStatus", "order002"); //입금대기
                    responseMap.put("usePayMethodName", "가상계좌");
                }
                // 07-1-4. 포인트
                if (use_pay_method.equals("000100000000")) {
                    responseMap.put("orderStatus", "order003"); //결제완료
                    responseMap.put("usePayMethodName", "포인트");
                }
                // 07-1-5. 휴대폰
                if (use_pay_method.equals("000010000000")) {
                    responseMap.put("orderStatus", "order003"); //결제완료
                    responseMap.put("usePayMethodName", "휴대폰");
                }
                // 07-1-6. 상품권
                if (use_pay_method.equals("000000001000")) {
                    responseMap.put("orderStatus", "order003"); //결제완료
                    responseMap.put("usePayMethodName", "상품권");
                }

                orderService.createPayment(responseMap, pgId);
                bSucc = orderService.createOrderSheet(responseMap, request);


                if (!StringUtils.equals("cash_no", "")) {
                    Map<String, Object> storeCashReceiptMap = new HashMap<>();
                    storeCashReceiptMap.put("orderSheetId", ordr_idxx);
                    storeCashReceiptMap.put("cashNo", cash_no);
                    storeCashReceiptMap.put("receiptNo", cash_authno);
                    storeCashReceiptMap.put("appTime", app_time);
                    storeCashReceiptMap.put("regStat", "NTNC");
                    storeCashReceiptMap.put("remMny", "국세청 등록완료");

                    /**
                     * orderSheet 데이터를 조회하기 때문에 생성된 후에 진행해야 합니다.
                     */
                    orderService.createCashReceipt(storeCashReceiptMap);
                }

            } catch (Exception e) {
                bSucc = "false";
            }
        }

        /* = -------------------------------------------------------------------------- = */
        /* =   07-2. 승인 실패 DB 처리(res_cd != "0000")                                = */
        /* = -------------------------------------------------------------------------- = */

        if (!"0000".equals(res_cd)) {
            bSucc = "false";
        }
    }
    /* = -------------------------------------------------------------------------- = */
    /* =   07. 승인 및 실패 결과 DB 처리 END                                        = */
    /* = ========================================================================== = */


    /* = ========================================================================== = */
    /* =   08. 승인 결과 DB 처리 실패시 : 자동취소                                  = */
    /* = -------------------------------------------------------------------------- = */
    /* =      승인 결과를 DB 작업 하는 과정에서 정상적으로 승인된 건에 대해         = */
    /* =      DB 작업을 실패하여 DB update 가 완료되지 않은 경우, 자동으로          = */
    /* =      승인 취소 요청을 하는 프로세스가 구성되어 있습니다.                   = */
    /* =                                                                            = */
    /* =      DB 작업이 실패 한 경우, bSucc 라는 변수(String)의 값을 "false"        = */
    /* =      로 설정해 주시기 바랍니다. (DB 작업 성공의 경우에는 "false" 이외의    = */
    /* =      값을 설정하시면 됩니다.)                                              = */
    /* = -------------------------------------------------------------------------- = */

    // 승인 결과 DB 처리 에러시 bSucc값을 false로 설정하여 거래건을 취소 요청

    if (req_tx.equals("pay")) {
        if (res_cd.equals("0000")) {
            if (bSucc.equals("false")) {
                int mod_data_set_no;

                c_PayPlus.mf_init_set();

                tran_cd = "00200000";

                mod_data_set_no = c_PayPlus.mf_add_set("mod_data");

                c_PayPlus.mf_set_us(mod_data_set_no, "tno", tno); // KCP 원거래 거래번호
                c_PayPlus.mf_set_us(mod_data_set_no, "mod_type", "STSC"); // 원거래 변경 요청 종류
                c_PayPlus.mf_set_us(mod_data_set_no, "mod_ip", cust_ip); // 변경 요청자 IP
                c_PayPlus.mf_set_us(mod_data_set_no, "mod_desc", "가맹점 결과 처리 오류 - 가맹점에서 취소 요청"); // 변경 사유

                c_PayPlus.mf_do_tx(g_conf_site_cd, g_conf_site_key, tran_cd, "", ordr_idxx, g_conf_log_level, "1");

                res_cd = c_PayPlus.m_res_cd;                                 // 결과 코드
                res_msg = c_PayPlus.m_res_msg;                                // 결과 메시지
            }
        }
    }
    // End of [res_cd = "0000"]
    /* = -------------------------------------------------------------------------- = */
    /* =   08. 승인 결과 DB 처리 END                                                = */
    /* = ========================================================================== = */


    /* ============================================================================== */
    /* =   09. 폼 구성 및 결과페이지 호출                                           = */
    /* -----------------------------------------------------------------------------= */
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>*** NHN KCP [AX-HUB Version] ***</title>
    <%
        if (res_cd.equals("0000")) {
    %>
    <script type="application/javascript">
        function goResult() {
//                    var openwin = window.open('proc_win.html', 'proc_win', '');
            document.pay_info.submit();
//                    openwin.close();


//                    alert('success');
        }
    </script>
    <%
    } else {
    %>
    <script type="application/javascript">
        function goResult() {
//                    var openwin = window.open('proc_win.html', 'proc_win', '');
            alert('<%=res_msg%>');
//                    document.pay_info.submit();
//                    openwin.close();
        }
    </script>

    <%
        }
    %>

    <script type="text/javascript">
        // 결제 중 새로고침 방지 샘플 스크립트
        function noRefresh() {
            /* CTRL + N키 막음. */
            if ((event.keyCode == 78) && (event.ctrlKey == true)) {
                event.keyCode = 0;
                return false;
            }
            /* F5 번키 막음. */
            if (event.keyCode == 116) {
                event.keyCode = 0;
                return false;
            }
        }

        document.onkeydown = noRefresh;
    </script>
</head>

<%

    logger.info("-----------------결제정보(" + ordr_idxx + ")------------------");
    logger.info("사이트 코드 : " + g_conf_site_cd);
    logger.info("프론트최종 결제금액(this.finalPrice) : " + good_mny);
    logger.info("요청구분 : " + req_tx);
    logger.info("사용한 결제 수단 : " + use_pay_method);
    logger.info("쇼핑몰 DB 처리 성공 여부 : " + bSucc);
    logger.info("KCP 실제 거래 금액 : " + amount);
    logger.info("복합결제시 총 거래금액 : " + total_amount);
    logger.info("카드결제금액 " + card_mny);
    logger.info("계좌이체결제금액 " + bk_mny);
    logger.info("쿠폰금액 " + coupon_mny);
    logger.info("결과 코드 : " + res_cd);
    logger.info("결과 메세지 : " + res_msg);
    logger.info("주문번호 : " + ordr_idxx);
    logger.info("KCP 거래번호 : " + tno);
    logger.info("상품명 : " + good_name);
    logger.info("주문자명 : " + buyr_name);
    logger.info("주문자 전화번호 : " + buyr_tel1);
    logger.info("주문자 휴대폰번호 : " + buyr_tel2);
    logger.info("주문자 E-mail : " + buyr_mail);
    logger.info("승인시간 : " + app_time);
    logger.info("카드코드 : " + card_cd);
    logger.info("카드이름 : " + card_name);
    logger.info("승인번호 : " + app_no);
    logger.info("무이자여부 : " + noinf);
    logger.info("할부개월 : " + quota);
    logger.info("부분취소가능여부 : " + partcanc_yn);
    logger.info("카드구분1 : " + card_bin_type_01);
    logger.info("카드구분2 : " + card_bin_type_02);
    logger.info("은행명 : " + bank_name);
    logger.info("은행코드 : " + bank_code);
    logger.info("입금 은행 : " + bankname);
    logger.info("입금계좌 예금주 : " + depositor);
    logger.info("입금계좌 번호 : " + account);
    logger.info("가상계좌 입금마감시간 : " + va_date);
    logger.info("포인트 서비스사 : " + pnt_issue);
    logger.info("승인시간 : " + pnt_app_time);
    logger.info("승인번호 : " + pnt_app_no);
    logger.info("적립금액 or 사용금액 : " + pnt_amount);
    logger.info("발생 포인트 : " + add_pnt);
    logger.info("사용가능 포인트 : " + use_pnt);
    logger.info("총 누적 포인트 : " + rsv_pnt);
    logger.info("통신사 코드 : " + commid);
    logger.info("휴대폰 번호 : " + mobile_no);
    logger.info("발급사 코드 : " + tk_van_code);
    logger.info("승인 번호 : " + tk_app_no);
    logger.info("가맹점 고객 아이디 " + shop_user_id);
    logger.info("현금영수증 등록 여부 : " + cash_yn);
    logger.info("현금 영수증 승인 번호 : " + cash_authno);
    logger.info("현금 영수증 발행 구분 : " + cash_tr_code);
    logger.info("현금 영수증 등록 번호 : " + cash_id_info);
    logger.info("현금 영수증 거래 번호 : " + cash_no);
    logger.info("----------------------------------------------------------");

%>

<body onload="goResult()">
<%
    if (res_cd.equals("0000")) {
%>
<form name="pay_info" method="post" action="http://10.10.90.31:3091/<%=siteId%>/order/complete">
    <%--<form name="pay_info" method="post" action="http://test.ygoon.com/<%=siteId%>/order/complete" target="_parent">--%>
    <input type="hidden" name="ordr_idxx" value="<%= ordr_idxx        %>">    <!-- 주문번호 -->
</form>
<%
} else {
%>
<form name="pay_info" method="get" action="http://10.10.90.31:3091/<%=siteId%>/order/<%=ordr_idxx%>"></form>
<%--<form name="pay_info" method="post" action="http://test.ygoon.com/<%=siteId%>/order/<%=ordr_idxx%>" target="_parent"></form>--%>
<%
    }
%>
</body>
</html>