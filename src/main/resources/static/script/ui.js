$(document).ready(function(){
    header();              // 헤더 레이아웃
    module();              // 모듈화 요소동작
    wing();                // 오른쪽 따라다니는 메뉴
	category_init();       // 카테고리 메인(2DEPTH), 서브(3DEPTH)
    goods_detail();        // 상품상세
    member();              // 회원, 로그인
    cs();                  // 고객센터
    cart();                // 장바구니
    tot_srch();            // 통합검색
    mypage();              // 마이페이지
    promotion();              // 프로모션 (기획전, 베스트)
    ui();
    
    member_event();
});

function promotion(){
    $(".promo_slider_wrap .slider").slick({ dots : true });
    $(".evt_slider_wrap .slider").slick({ dots : true });
}

function mypage(){
    // 작성한 상품평 보기
    $(".info_td.goods_rating p.rating_comment a").on("click", function(e){
        e.preventDefault();
        var idx = $(this).index(".info_td.goods_rating p.rating_comment a");
        
        if($(".rating_memo_tr").eq(idx).hasClass("on")){
            $(".rating_memo_tr").removeClass("on");
            $(".info_td.goods_rating p.rating_comment").removeClass("on");
        }else{
            $(".rating_memo_tr").removeClass("on").eq(idx).addClass("on");   
            $(".info_td.goods_rating p.rating_comment").removeClass("on").eq(idx).addClass("on"); 
        }
    });
    
    // 상품 QA
    $(".q_info.answer_done .qa_title").on("click", function(e){
        e.preventDefault();
        var idx = $(this).index(".q_info.answer_done .qa_title");
        
        if($(".qa_tr").eq(idx).hasClass("on")){
            $(".qa_tr").removeClass("on");
            $(".q_info.answer_done .qa_title").removeClass("on");
        }else{
            $(".qa_tr").removeClass("on").eq(idx).addClass("on"); 
            $(".q_info.answer_done .qa_title").removeClass("on").eq(idx).addClass("on");   
        }
    });
}

function tot_srch(){        
    $(".tot_srch_tab_wrap .tab_nav .slide").each(function(idx){
        console.log(idx);
        
        if($("> div", $(this)).length > 5){
            $(this).slick({
                infinite: false,
                slidesToShow: 5,
                slidesToScroll: 1
            }); 
        }
    });
    
}

function cart(){
    
    // 쿠폰 팝업
    $("a.btn.sml.coupon").on("click", function(e){
        e.preventDefault();
        var $this = $(this), $tooltip_popup = $this.parent().find(".tooltip_popup");
        $(".tooltip_popup").hide();
        $tooltip_popup.show();
        $tooltip_popup.find(".tooltip_close a").on("click", function(e){
            e.preventDefault();
            $tooltip_popup.hide();
        });
    });
}

// 고객센터
function cs(){
    
    // FAQ 탭변경
    $(".faq_cat_tab_nav ul li").on("click", function(e){
        e.preventDefault();
        var $this = $(this),
            idx = $(this).index(".faq_cat_tab_nav ul li");
        $(".faq_cat_tab_nav ul li").removeClass("on").eq(idx).addClass("on");
        $(".faq_cat_tab_contents").addClass("hide").eq(idx).removeClass("hide");
    });
    
    // FAQ 상세보기 아코디언
    $(".faq_list li").on("click", function(e){
        e.preventDefault();
        var idx = $(this).index(".faq_list li"), lnk = $(".faq_list li");

        if(lnk.eq(idx).hasClass("on")){
            lnk.removeClass("on");
        }else{
            lnk.removeClass("on").eq(idx).addClass("on");
        }
    });
    
    // 1:1 문의하기 문의유형 선택
    $("select.sel.faq_type").change(function(e){
        var $this = $(this), val = $this.val();

        if(val == "1"){
            // 주문/배송/결제
            $(this).parent().addClass("on");
            $(".sel_inquiry_goods").addClass("on");
        }else if(val == "2"){
            // 취소/반품/교환/환불
            $(this).parent().addClass("on");
            $(".sel_inquiry_goods").addClass("on");
        }else if(val == "3"){
            // 회원혜택
            $(this).parent().addClass("on");
            $(".sel_inquiry_goods").removeClass("on");
        }else if(val == "4"){
            // 모바일
            $(this).parent().addClass("on");
            $(".sel_inquiry_goods").removeClass("on");
        }else if(val == "5"){
            // 기타
            $(this).parent().addClass("on");
            $(".sel_inquiry_goods").removeClass("on");
        }else{
            // 초기화
            $(this).parent().removeClass("on");
            $(".sel_inquiry_goods").removeClass("on");
        }
    });
    
    // 1:1 문의하기 메일선택
    $("select.sel.email").change(function(e){
        var $this = $(this), val = $this.val();

        if(val == "direct"){
            // 직접입력 시
            $this.prev("input.txt.hide").removeClass("hide").focus();   
        }else{
            // 도메인 선택 시
            $this.prev("input.txt").addClass("hide");
            $this.focus();  
        }
    });
    
}

// 회원, 로그인
function member(){
    // 대학검색 아코디언
    $(".uni_srch_inbox .uni_nm > a").on("click", function(e){
        e.preventDefault();
        var idx = $(this).index(".uni_srch_inbox .uni_nm > a"),
            lnk = $(".uni_srch_inbox");

        if(lnk.eq(idx).hasClass("on")){
            lnk.removeClass("on");
        }else{
            lnk.removeClass("on").eq(idx).addClass("on");
        }
    });
    
    // 아이디 찾기 인증번호 전송
    $(".btn_certi").on("click", function(e){
        e.preventDefault();
        var $this = $(this);
        $(".certi_no").attr("readonly", false);
        $this.addClass("hide");
        $(".remain_time").removeClass("hide");
    });
    
    // 비밀번호 찾기 (휴대폰으로 찾기 선택 시)
    $(".find_method input.rdo").on("click", function(e){
        var idx = $(this).index(".find_method input.rdo");
        if(idx == 1){
            $(".srch_info_inbox.find_info").removeClass("hide");
        }else{
            $(".srch_info_inbox.find_info").addClass("hide");
        }
    });
    
    // 비밀번호 찾기 버튼
    $(".btn_find_pwd").on("click", function(e){
        e.preventDefault();
        
        // 이메일 찾기로 선택 시
        if($(".find_method input.rdo").eq(0).is(":checked")){
            var $obj = $(".modal.find_pw_email");
            modal_open($obj);
        }
    });
}

// 기본 UI
function ui(){
    // 버튼으로 모달 팝업 열기
    // <a href="#" class="btn_modal" data-src="sign_up">회원가입 모달</a>
    $(".btn_modal").off("click").on("click", function(e){
        e.preventDefault();
        var call_modal = $(this).attr("data-src"),
            $call_modal = $(".modal."+call_modal),
            $call_modal_layer = $(".modal_layer", $call_modal),
            $call_modal_close = $(".modal_close a", $call_modal);

        $call_modal.addClass("on");
        $call_modal_layer.css({ 
            'margin-top' :  - ($call_modal_layer.outerHeight() / 2),
            'margin-left' : - ($call_modal_layer.outerWidth() / 2)
        });
        
        $(".modal_close a, .btn.close, .btn_wrap a.cancel", $call_modal).on("click", function(e){
            e.preventDefault(); 
            $(this).closest(".modal").removeClass("on");
        });
    });
    
    // 탭내용 변경 공통
    $(".tab_with_contents ul li").off("click").on("click", function(e){
        e.preventDefault();
        var $this = $(this), idx = $this.index(".tab_with_contents ul li");
        
        $this.closest(".tab_with_contents").find("li").removeClass("on").eq(idx).addClass("on");
        $this.closest(".tab_wrap").find(".tab_contents").addClass("hide").eq(idx).removeClass("hide");
        
    });
}

// 객체로 접근해서 모달 팝업 열기
function modal_open($obj){
    if($obj.length){
        var $modal_layer = $(".modal_layer", $obj);
        $obj.addClass("on");
        $modal_layer.css({ 
            'margin-top' :  - ($modal_layer.outerHeight() / 2),
            'margin-left' : - ($modal_layer.outerWidth() / 2)
        });
        modal_close($obj);
    }
}

// 모달 팝업 닫기
function modal_close($obj){
    $(".modal_close a, .btn.close, .btn_wrap a.cancel", $obj).on("click", function(e){
        e.preventDefault(); 
        $(this).closest(".modal").removeClass("on");
    });
}

// 상품상세
function goods_detail(){
    
    // 2017-09-06 추가
    $('#stars li').on('mouseover', function(){
        var onStar = parseInt($(this).data('value'), 10);
        $(this).parent().children('li.star').each(function(e){
            if (e < onStar){
                $(this).addClass('hover');
            }else{
                $(this).removeClass('hover');
            }
        });
    }).on('mouseout', function(){
            $(this).parent().children('li.star').each(function(e){
            $(this).removeClass('hover');
        });
    });

    $('#stars li').on('click', function(){
        var onStar = parseInt($(this).data('value'), 10);
        var stars = $(this).parent().children('li.star');
        for (i = 0; i < stars.length; i++) $(stars[i]).removeClass('selected');
        for (i = 0; i < onStar; i++) $(stars[i]).addClass('selected');
        var ratingValue = parseInt($('#stars li.selected').last().data('value'), 10);
        console.log(ratingValue);
    });
    // 2017-09-06 추가
    
    
    // 옵션선택 하단 고정
    // 스크롤이 구매버튼 지나가면 노출, 콘텐츠 영역 끝날때 숨김.
    if($(".thumb_info_box").length){
        
        $("#contents").waitForImages(function(){
            var $scroll_box = $(".thumb_info_box"),
                box_top = $scroll_box.offset().top,
                box_h = $scroll_box.outerHeight(),
                scroll_show_top = box_top + box_h - $(".header_fixed").outerHeight() * 2,
                scroll_hide_px = $("#contents").height() + $("#contents").offset().top,
                scroll_hide_px2 = $("#footer").position().top;

            $(window).scroll(function(){
                var scroll_top = $(window).height() + $(window).scrollTop();      
                //console.log(scroll_top + " : " + scroll_hide_px2);
                if($(window).scrollTop() > scroll_show_top){
                    $(".floating_buy_wrap").addClass("on");
                    
                    if(scroll_top > scroll_hide_px2){
                        $(".floating_buy_wrap").removeClass("on");
                    }

                }else{
                    $(".floating_buy_wrap").removeClass("on");
                }
            });
            
            $(".thumb_list ul li a:not(.thumb_list ul li a.video_thumb)").on("click", function(e){
                e.preventDefault();
                var $this = $(this);
                    img_src = $this.attr("data-src");
                $(".thumb_list ul li").removeClass("on");
                $this.parent().addClass("on");
                $(".thumb_big img").attr("src", img_src);
            });
        });
        
        
        // 옵션선택 활성화 / 비활성화
        $(".floating_buy_wrap .float_btn a").on("click", function(e){
            e.preventDefault();
            $(this).toggleClass("on");
            $(".floating_buy_wrap .float_buy_box").toggleClass("on");
        });

        // 상세정보 탭클릭
        $(".goods_detail_tab_nav ul li a").on("click", function(e){
            e.preventDefault();
            /*var idx = $(this).index(".goods_detail_tab_nav ul li");
            $(".goods_detail_tab_nav ul li").removeClass("on").eq(idx).addClass("on");
            $(".goods_detail_tap_contents").addClass("hide").eq(idx).removeClass("hide");*/
            
            var $this = $(this), href = $this.attr("href"), top = $(href).offset().top;
            
            //console.log(top - $("#header_fixed").outerHeight() - $(".goods_detail_tab_nav").outerHeight());
            
            top -= ($("#header_fixed").outerHeight() + $(".goods_detail_tab_nav").outerHeight() + 60);
            
            $("html, body").animate({
                scrollTop : top
            }, "slow");
        });
        
        // 구매평 아코디언
        $(".rating_list li.rating_link > a").on("click", function(e){
            e.preventDefault();
            var idx = $(this).index(".rating_list li.rating_link > a"),
                lnk = $(".rating_list li.rating_link");
            
            if(lnk.eq(idx).hasClass("on")){
                lnk.removeClass("on");
            }else{
                lnk.removeClass("on").eq(idx).addClass("on");
            }
        });
        
        // 구매평 쓰기
        $(".rating_btn a").on("click", function(e){
            e.preventDefault();
            $(".rating_write_box").toggleClass("on");
        });
        
        // Q&A 아코디언
        $(".qa_list li.qa_link > a").on("click", function(e){
            e.preventDefault();
            var idx = $(this).index(".qa_list li.qa_link > a"),
                lnk = $(".qa_list li.qa_link");
            
            if(lnk.eq(idx).hasClass("answer_done")){            
                if(lnk.eq(idx).hasClass("on")){
                    lnk.removeClass("on");
                }else{
                    lnk.removeClass("on").eq(idx).addClass("on");
                }
            }
        });
        
        // 툴팁팝업
        $(".info_items .etc a:not(.tooltip_popup a)").on("click", function(e){
            e.preventDefault();
            var $this = $(this), $tooltip_popup = $this.parent().find(".tooltip_popup");
            $(".tooltip_popup").hide();
            $tooltip_popup.show();
            $tooltip_popup.find(".tooltip_close a").on("click", function(e){
                e.preventDefault();
                $tooltip_popup.hide();
            });
        });
    }
}

// 카테고리 메인(2DEPTH), 서브(3DEPTH)
function category_init(){
    if($(".cat_promotion").length){
        // 카테고리 메인
        // 프로모션 슬라이드 옵션
        var cat_promotion_opt = {
            slidesToShow : 1
            ,slidesToScroll : 1
            ,arrow : false
            ,autoplay : true
            ,autoplaySpeed : 5000
            ,pauseOnFocus : true
            ,pauseOnHover : true
            ,pauseOnDotsHover : true
        };
        var cat_promotion_slick = $(".cat_promotion .slider_for").slick(cat_promotion_opt)
            ,$cat_promotion_li = $(".cat_promotion_nav.slider_nav ul li")

        cat_promotion_slick.on('afterChange', function(event, slick, currentSlide, nextSlide){
            $cat_promotion_li.removeClass("on");
            $cat_promotion_li.eq(currentSlide).addClass("on");
        });

        // 프로모션 링크 클릭 - 슬라이드 이동
        $("a", $cat_promotion_li).each(function(index){
            var $this = $(this);
            $this.click(function(e){
                e.preventDefault();
                if($this.parent().hasClass("slider_nav")){
                    if($this.hasClass("play")){
                        cat_promotion_slick.slick('slickPlay');
                        $(".play").addClass("hide");
                        $(".stop").removeClass("hide");
                    }else if($this.hasClass("stop")){
                        cat_promotion_slick.slick('slickPause');
                        $(".play").removeClass("hide");
                        $(".stop").addClass("hide");
                    }
                }else{
                    cat_promotion_slick.slick('slickGoTo', index);
                    $(".cat_promotion_nav.slider_nav ul li").removeClass("on");
                    $(this).parent().addClass("on");
                }
            });
        });
    }
    
    
    // 카테고리 서브
    // 상세검색 옵션 항목 더 보기
    $(".srch_sel .opt_view a").on("click", function(e){
        e.preventDefault();
        var $this = $(this);
        if($this.attr("class").indexOf("opt_more") > -1){
            $this.parent().parent().addClass("on");
            $this.addClass("hide");
            $this.parent().find(".opt_hide").removeClass("hide");
        }else{
            $this.parent().parent().removeClass("on");
            $this.addClass("hide");
            $this.parent().find(".opt_more").removeClass("hide");
        }
    });

    // 상세검색 항목 클릭
    $(".srch_sel label input").on("click", function(e){
        var $this = $(this);
        var sel_txt = $this.next("span").text()
            ,data_val = $this.attr("data-val")
            ,li_html = '<li data-val="'+data_val+'"><span>'+ sel_txt +'</span><a href="#" class="del_sel"><span>삭제</span></a></li>';

        if($this.is(":checked")){                                        
            $(".detail_srch_selected .sel_list ul").append(li_html);

            // 상세검색 항목 삭제
            $(".detail_srch_selected .del_sel").on("click", function(e){
                e.preventDefault();
                var $this = $(this), $par_li = $this.closest("li"), data_val = $par_li.attr("data-val");
                $par_li.remove();
                $(".srch_sel label input[data-val="+data_val+"]").prop("checked", false);
                if($(".detail_srch_selected .sel_list li").length == 0){
                }
            });
        }else{
            $(".detail_srch_selected .sel_list ul li[data-val="+ data_val +"]").remove();
        }
    });

    // 초기화
    $(".detail_srch_selected .clear_all a").on("click", function(e){
        e.preventDefault();
        $(".srch_sel label input").prop("checked", false);
        $(".detail_srch_selected .sel_list ul li").remove();
    });
}

// 모듈
function module(){
    // SINGLE SLIDE
	if($(".module.h_slide").length){
		var h_slide_opt = {
			slidesToShow: 6,
			slidesToScroll: 1,
			dots: false,
			infinite: true,
			focusOnSelect: true,
			variableWidth: true
		}
		var h_slide_slick = $(".module.h_slide .slider_wrap .slider").slick(h_slide_opt);
	}	
    
    
    // VERTICAL TAB - BEST ITEM 
    if($(".vtc_tab_wrap").length){

        $(".vtc_tab_wrap .tab_nav .slide_nav").slick({
            vertical : true
            ,infinite : false
            ,slidesToShow: 5
            ,slidesToScroll: 1
        });

        $(".vtc_tab_wrap .tab_nav .slide_nav .item").on("click", function(e){
            e.preventDefault();
            var $this = $(this),
                idx = $(this).attr("data-slick-index");
                console.log(idx);
            $(".vtc_tab_wrap .tab_nav .slide_nav .item").removeClass("on").eq(idx).addClass("on");
            $(".vtc_tab_wrap .tab_content_wrap .tab_content").addClass("hide").eq(idx).removeClass("hide");    
        });
    }
    
    
    // LABEL TAB : BEST PRICE
    if($(".label_tab_wrap").length){
        $(".label_tab_wrap").each(function(){
            var $this = $(this)
                ,li_len = $(".tab_nav li", $this).length
                ,li_h = $this.height() / li_len;

            $(".tab_nav", $this).addClass("cnt_" + li_len);

            $(".tab_nav li a", $this).each(function(idx){                                            
                $(this).css({
                    height : li_h
                });

                $(this).on("click", function(e){
                    e.preventDefault();
                    $(".tab_nav li", $this).removeClass("on").eq(idx).addClass("on");
                    $(".tab_content_wrap .tab_content", $this).addClass("hide").eq(idx).removeClass("hide");    
                });
            });
        });
    }
}

// 오른쪽 고정메뉴
function wing(){
	// 최근본상품
	if($(".recent_view").length){
		// 로딩 시 최근 본 상품
		var $recent_view = $(".recent_view_list ul"),
			$recent_view_list = $("li", $recent_view),
			list_per_page = 3,	// 한페이지 보여질 항목 수
			cur_pge = 1,		// 현재 페이지 기본값 1
			tot_page = 1;		// 총 페이지 기본값 1
		
		$(".recent_view_title span").text($recent_view_list.length);			// 총 최근 본 상품 수
		tot_page = parseInt($recent_view_list.length / 3, 10) + 1;			// 총 페이지 수

		// 2014-12-24 추가
		if($recent_view_list.length == 0){
			tot_page = 1;
			$(".recent_view_list_nodata").removeClass("hide");
		}else{
			if(parseInt($recent_view_list.length % 3, 10) == 0) tot_page -= 1;
		}

		$(".recent_view_foot .recent_view_info span").text(" / "+ tot_page);
		$("li:gt(2)", $recent_view).css({'display':'none'});					// 3번째 이상부터는 display:none 처리
		$recent_view.css({'display':'block'});									// 숨겨놨던 실제 리스트 보이기

		// 최근 본 상품 삭제
		$(".recent_view_del").click(function(e){
			e.preventDefault();
			var idx = $(this).index(".recent_view_del");							
			var view_count = parseInt($(".recent_view_title span").text(), 10);
			$(".recent_view_title span").text(view_count - 1);
			
			// 현재 보여지고 있는 항목 수
			var visible_cnt = $("li:visible", $recent_view).length;
			if(visible_cnt == 1){
				// 현재 페이지에 최근 본 상품 한개만 보이면.
				var cur_page = parseInt($(".recent_view_foot .recent_view_info strong").text());
				cur_page -= 1;
				$(".recent_view_list ul li").css({'display':'none'});
				for(var i = (cur_page * list_per_page - list_per_page); i < cur_page * list_per_page; i++){
					$(".recent_view_list ul li").eq(i).css({'display':'block'});
				}
			}else{
				// 한개 이상일 때
				$("li:gt("+idx+"):hidden:first", $recent_view).css({'display':'block'});
			}

			$(this).parent("li").remove();	// 해당항목 삭제

			// 전체 페이지 수 재계산
			tot_page = Math.ceil($("li", $recent_view).length / 3);
			if(tot_page == 0){
				tot_page = 1;
				cur_page = 1;
				$recent_view.addClass("hide");
				$(".recent_view_list_nodata").removeClass("hide");
			}
			$(".recent_view_foot .recent_view_info span").text(" / "+ tot_page);
			$(".recent_view_foot .recent_view_info strong").text(cur_page);
		});

		// 최근 본 상품 이전 페이지
		$(".recent_view_foot a.recent_view_prev").on("click", function(e){
			e.preventDefault();
			var cur_page = parseInt($(".recent_view_info strong").text(), 10), prev_cur_page = cur_page;
			//console.log(cur_page - 1 == 0);
			if(cur_page - 1 == 0){
				cur_page = tot_page;
			}else{
				cur_page -= 1;
			}
			prev_cur_page = cur_page - 1;

			$(".recent_view_list ul li").css({'display':'none'});
			for(var i = prev_cur_page * list_per_page; i < cur_page * list_per_page; i++){
				$(".recent_view_list ul li").eq(i).css({'display':'block'});
			}
			$(".recent_view_info strong").text(cur_page);
		});
		
		// 최근 본 상품 다음 페이지
		$(".recent_view_foot a.recent_view_next").on("click", function(e){
			e.preventDefault();
			var cur_page = parseInt($(".recent_view_info strong").text(), 10), prev_cur_page = cur_page;
			if(cur_page + 1 > tot_page){
				cur_page = 1;
				prev_cur_page = 0;
			}else{
				cur_page += 1;
			}

			$(".recent_view_list ul li").css({'display':'none'});
			for(var i = prev_cur_page * list_per_page; i < cur_page * list_per_page; i++){
				$(".recent_view_list ul li").eq(i).css({'display':'block'});
			}
			$(".recent_view_info strong").text(cur_page);
		});
	}
}

// 헤더 관련 모음
function header(){
	
	// 헤더 상단 롤링배너
    var $header_rolling = $(".banner_area")
		,$header_rolling_prev = $(".banner_area .prev")
		,$header_rolling_next = $(".banner_area .next")
		,$rolling_list = $(".banner_area li")
		,$rolling_cnt = $rolling_list.length
		,$cur_cnt = $(".cur", $header_rolling)
		,cur = parseInt($cur_cnt.text(), 10);
	

	$header_rolling.waitForImages(function(){
		$header_rolling.addClass("on");
    	$(".tot", $header_rolling).text("/" + $rolling_cnt);

		$header_rolling_prev.click(function(){
			if(cur - 1 == 0){
				cur = $rolling_cnt
			}else{
				cur -= 1;
			}
			$rolling_list.addClass("hide").eq(cur-1).removeClass("hide");
			$cur_cnt.text(cur);
		});

		$header_rolling_next.click(function(){
			if(cur + 1 > $rolling_cnt){
				cur = 1
			}else{
				cur += 1;
			}
			$rolling_list.addClass("hide").eq(cur-1).removeClass("hide");
			$cur_cnt.text(cur);
		});
	});
	
	// 스크롤 메뉴
	$(window).on("scroll", function(){
		var scroll_t = $(window).scrollTop()
			,fix_h = $(".top_banner").outerHeight() + $("#header .util").outerHeight() + $("#header .other").outerHeight() + $(".nav").outerHeight() - 10
			,$fixed_header = $(".header_fixed");
		
		if(scroll_t > fix_h){ 
			$fixed_header.show();
			
			// 전체카테고리 열려있으면 스크롤 따라 올라오기
			if($(".view_all_cat_box").hasClass("on")){
				$(".view_all_cat_box").css({ top : $(window).scrollTop() });
			}
		}else{
			$fixed_header.hide();
			
			// 전체 카테고리 펼쳐져 있는 상태에서 스크롤 메뉴 숨김시 GNB 전체카테고리 처럼 띄우기
			if($(".view_all_cat_box").hasClass("on")){
				var offset_t = $(".btn_view_all").offset().top
				$(".view_all_cat_box").css({ top : offset_t - 20 });
			}
		}		
	});
    
    // 전체카테고리
	var $btn_view_all = $(".btn_view_all, .fixed_view_all")
		,$all_cat_box = $(".view_all_cat_box");
	
    $btn_view_all.on("click", function(e){
        e.preventDefault();
		var offset_t = $(this).offset().top
        $btn_view_all.toggleClass("on");
        $all_cat_box.toggleClass("on");
		
		if($(this).hasClass("btn_view_all")){
			offset_t -= 20;
		}
		$(".view_all_cat_box").css({ top : offset_t });
    });
}

function member_event(){
	// 인증영역 토클
	$('.select_inz_method ul li label').each(function(){
		$(this).click(function(){
			var elem = 	$(this).index('.select_inz_method ul li label');
			$('.select_cont_block').removeClass('select');
			$('.select_cont_block').eq(elem).addClass('select');
		});
	});
	// 검색 토글 
	$(".search_result_list.college li a.keyword").on("click", function(e){
        e.preventDefault();
        var $this = $(this), $this_p = $this.parent('li'), $accordian_con = $this_p.find(".search_detail_box");
        
        if($this_p.hasClass("open")){
			$accordian_con.slideUp(300);
            $this_p.removeClass("open");
        } else{
            $accordian_con.slideDown(300);
            $this_p.addClass("open");
        }
    });	
	// 회원가입 아이템 선택
	$('.item_checks').each(function(){
		$(this).find('button').click(function(){
			if ($(this).hasClass('on')){
				$(this).removeClass('on');	
			} else {
				$(this).addClass('on');
			}			
		});
	});
	// 회원가입 약관동의 토글
	$('.agree_area dt input').click(function(){
		if ($(this).hasClass('on')){
			$(this).removeClass('on');	
			$('.agree_area dd').hide();
		} else {
			$(this).addClass('on');
			$('.agree_area dd').show();
		}			
	});
	$('.agree_area dd ul > li > span input').click(function(){
		if ($(this).hasClass('on')){
			$(this).removeClass('on');	
			$(this).parents('li').find('.agree_box').hide();
		} else {
			$(this).addClass('on');
			$(this).parents('li').find('.agree_box').show();
		}			
	});
}