<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.lang.management.*,java.lang.Thread.State,java.util.Map,java.util.HashMap,java.util.HashSet"%>
<%@page import="java.text.DecimalFormat,java.util.Date,java.util.List"%>
<%@page import="java.io.*"%>

<%!
/************** page variable and method define start ************************/
	final static String versionAndTitle="Thread Checker include GC information v.2011.07.09.";	
	final static int[] optionSeconds={1,3,5,10,20,30,60,600};
	final static String numberPattern="###,###,###,###.#";
	final static DecimalFormat format=new DecimalFormat(numberPattern);
	
	final static String TIME_MS=" ms";
	final static String TIME_SEC=" 초";
	final static String TIME_MIN=" 분";
	public String getTimeString(long nanoSecond) {
		long tempMilli=nanoSecond/1000000;
		if(tempMilli<1000) {
			return format.format(tempMilli/1000.0)+TIME_MS;
		} else if(tempMilli<60000) {
			return format.format(tempMilli/1000.0) +TIME_SEC;
		} else  {
			return format.format(tempMilli/60000.0) +TIME_MIN;
		} 
	}
	final static String SIZE_BYTES=" Bytes";
	final static String SIZE_KB=" kb";
	final static String SIZE_MB=" mb";
	final static String memoryPattern="###,###.#";
	final static DecimalFormat memoryFormat=new DecimalFormat(memoryPattern);
	public String getMemoryString(long bytes) {
		if(bytes<1024) {
			return memoryFormat.format(bytes)+SIZE_BYTES;
		} else if(bytes<1024*1024) {
			return memoryFormat.format(bytes/1024) +SIZE_KB;
		} else  {
			return memoryFormat.format(bytes/(1024*1024)) +SIZE_MB;
		} 
	}
	final static String pageWidth="1024";//"100%";
	final static String pageWidth2="1000";//"95%";
	final static int memLimitRate = 80; //80%
	
/************** page variable and method define end ************************/
	final static String STRING_STACK_TRACE_INFO_EXTENDED="<TR align=center ><TD width=\"40\" bgcolor=\"#E0E0E0\" class=\"txt\">Ordinal</TD>"
		+"<TD width=\"48\" bgcolor=\"#E0E0E0\" class=\"txt\">쓰레드 명</TD>"
		+"<TD width=\"65\" bgcolor=\"#E0E0E0\" class=\"txt\">쓰레드 상태</TD>"
		+"<TD width=\"81\" bgcolor=\"#E0E0E0\" class=\"txt\">쓰레드 수행시간</TD>"
		+"<TD width=\"88\" bgcolor=\"#E0E0E0\" class=\"txt\">Lock Owner</TD>"
		+"<TD width=\"343\" bgcolor=\"#E0E0E0\" class=\"txt\">Stack Trace information</TD></TR>"; 
	final static String STRING_STACK_TRACE_INFO_STANDARD="<TR align=center><TD width=\"40\" bgcolor=\"#E0E0E0\" class=\"txt\">Ordinal</TD>"
		+"<TD width=\"48\" bgcolor=\"#E0E0E0\" class=\"txt\">쓰레드 명</TD>"
		+"<TD width=\"65\" bgcolor=\"#E0E0E0\" class=\"txt\">쓰레드 상태</TD>"
		+"<TD width=\"81\" bgcolor=\"#E0E0E0\" class=\"txt\">쓰레드 수행시간</TD>";
%>
<HTML>
<HEAD>
	<TITLE><%=versionAndTitle %></TITLE>

<%
/************** request value check start ************************/
	String STRING_AUTO_REFRESH="autoRefresh";
	String STRING_REFRESH_SECOND="refreshSecond";
	String STRING_VIEW_ALL_THREAD_INFO="viewAllThreadInfo";
	String STRING_VIEW_LOCK_INFO="viewLockInfo";
	String STRING_STACK_TRACE_NUMBER="stackTraceNumber";
	boolean autoRefresh=false;
	String autoRefreshString="";
	String autoRefreshReceive=request.getParameter(STRING_AUTO_REFRESH);
	if(autoRefreshReceive!=null ) {
		autoRefresh=true;
		autoRefreshString=" checked ";
		//out.println("auto receive="+autoRefreshReceive);
	}
	
	
	int refreshSecond=3;
	String refreshSecondReceive=request.getParameter(STRING_REFRESH_SECOND);
	if(refreshSecondReceive!=null) {
		refreshSecond=Integer.parseInt(refreshSecondReceive);
	}
	
	boolean viewAllThreadInfo=false;
	String viewAllThreadInfoString="";
	String viewAllThreadInfoReceive=request.getParameter(STRING_VIEW_ALL_THREAD_INFO);
	if(viewAllThreadInfoReceive!=null) {
		viewAllThreadInfo=true;
		viewAllThreadInfoString=" checked ";
	}

	boolean viewLockInfo=false;
	String viewLockInfoString="";
	String viewLockInfoReceive=request.getParameter(STRING_VIEW_LOCK_INFO);
	if(viewLockInfoReceive!=null) {
		viewLockInfo=true;
		viewLockInfoString=" checked ";
	}
	int stackTraceNumber=100;
	String stackTraceNumberReceive=request.getParameter(STRING_STACK_TRACE_NUMBER);
	if(stackTraceNumberReceive!=null) {
		stackTraceNumber=Integer.parseInt(stackTraceNumberReceive);
	}
	
	
	
/************** request value check end ************************/
%>
<SCRIPT>
var t=setTimeout("refresh()",<%=refreshSecond*1000%>);
var refreshFlag=<%=autoRefresh%>;
function refresh() {
	if(refreshFlag==true) {
		document.options.submit();
	}
}
function changeOption() {
	document.options.submit();
}
function viewLockDetailTable(){
	if(document.options.viewAllThreadInfo.checked==true) {
		lockDetailInfo.style.visibility="visible";
	} else {
		lockDetailInfo.style.visibility="hidden";
	}
}
</SCRIPT>
<style type="text/css">
<!--
.txt {font-family: "consolas";	font-size: 12px;	color: #333333;}
.txtx {background-color:red;font-family: "consolas";font-size: 20px;color: white;font-weight: bold;}
.style2 {font-family:"consolas";font-size: 14px;color: #000066;font-weight: bold;}
.style5 {color: #C2C2C2}
.style6 {font-size: 10px}
-->
</style>
</HEAD>
<BODY onload="viewLockDetailTable()">

<!--<table width="<%=pageWidth %>" border="0" cellspacing="0">
  <tr>
    <td height="60" align="center" bgcolor="#B8C6CD">
    <table width="<%=pageWidth2 %>" border="0" cellspacing="0">
      <tr>
        <td height="44" align="center" bgcolor="#EBEBEB"><span class="style2"><%=versionAndTitle%> </span></td>
      </tr>
    </table>
    </td>
  </tr>
  <tr>
    <td height="4" align="center" bgcolor="#003399"></td>
  </tr>
  <tr>
    <td height="4" align="center" bgcolor="#ffffff"></td>
  </tr>
  <tr>
    <td height="2" align="center" bgcolor="#999999"></td>
  </tr>
</table> -->


<BR>
<FORM name="options" method="post">
<!---------------------- Top option part start ---------------------->
<TABLE width="<%=pageWidth %>">
<TR>
<TD width="713">
	<TABLE width="100%" border="0" cellpadding="5" cellspacing="1" bgcolor="#003399">
		<TR>
		<TD bgcolor="#FFFFFF">
		<button onclick="changeOption()">옵션 변경</button>
		</TD>
		<TD bgcolor="#FFFFFF" class="txt">
			<input type="checkbox" name="autoRefresh" <%=autoRefreshString %>> Auto Refresh
		</TD>
		<TD bgcolor="#FFFFFF" class="txt">Refresh rate:
			<SELECT name="refreshSecond">
<%
		for(int loop=0;loop<7;loop++) {
			out.print("<option value=");
			out.print(optionSeconds[loop]);
			if(refreshSecond==optionSeconds[loop]) {
				out.print(" selected ");
			}
			out.print(">");
			out.print(optionSeconds[loop]);
			out.println(" 초</option>\n");
		}
%>		
			</SELECT>
		</TD>
		<TD bgcolor="#FFFFFF" class="txt">
			<input type="checkbox" name="viewAllThreadInfo" onclick="viewLockDetailTable()" <%=viewAllThreadInfoString %>> View all threads
		</TD>
		</TR>
	</TABLE>
</TD><TD width="175" align="right">
	<DIV id="lockDetailInfo">
	<TABLE>
		<TR>
		<TD bgcolor="#FFFFFF" class="txt">
			<input type="checkbox" name="viewLockInfo" <% out.println(viewLockInfoString); %>> 
			View Lock Info
		</TD>
		<TD bgcolor="#FFFFFF" class="txt">
			<Select name="stackTraceNumber">
				<option value="100" <%= stackTraceNumber==100 ? "selected" : "" %>>ALL</option>
				<option value="1" <%= stackTraceNumber==1 ? "selected" : "" %>>1</option>
				<option value="5" <%= stackTraceNumber==5 ? "selected" : "" %>>5</option>
				<option value="10" <%= stackTraceNumber==10 ? "selected" : "" %>>10</option>
				<option value="20" <%= stackTraceNumber==20 ? "selected" : "" %>>20</option>
				<option value="30" <%= stackTraceNumber==30 ? "selected" : "" %>>30</option>
			</Select>
		</TD>
		</TR>
	</TABLE>
	</DIV>
</TD></TR></TABLE>
<!---------------------- Top option part end ---------------------->
</FORM>
<span class="style2">
<span class="style5">*</span>WAS Server name : <%=application.getServerInfo() %>(IP:<%= java.net.InetAddress.getLocalHost().getHostAddress() %>)  .&nbsp;&nbsp;&nbsp;
<span class="style5">*</span>최종 데이타출력시간 : <%=new Date() %>
</span><BR><BR>

<%

/*************************** Data Collect start *******************/
  //long periodMillis=1000;
	
	StringBuffer printDataString=new StringBuffer();
	int blocked=0;
	int newthread=0;
	int runnable=0;
	int terminated=0;
	int timed_waiting=0;
	int waiting=0;
	//this attribute is used to change Lock Owner's color
	HashSet<String> lockOwnerSet=new HashSet<String>();
	
	printDataString.append("<TABLE id=detailThreadInfoTable  width='800' ")
	.append(pageWidth)
	.append("\" border=0 cellpadding=\"5\" cellspacing=\"1\" bgcolor=\"#003399\">");
	/********************* check if I can see the lock info ********************************/
	if(viewLockInfo) {
		printDataString.append(STRING_STACK_TRACE_INFO_EXTENDED); 
	} else {
		printDataString.append(STRING_STACK_TRACE_INFO_STANDARD);
	}
	/********************* get ThreadMXBean info ********************************/
	ThreadMXBean tmxBean=ManagementFactory.getThreadMXBean();
	
	long threadList[]=tmxBean.getAllThreadIds();
	HashMap<Long,StackTraceElement[]> stackTraceMap=new HashMap<Long,StackTraceElement[]>();
	/********************* get GarbageCollectorMXBean info ********************************/
	List<GarbageCollectorMXBean> gcBeanList=ManagementFactory.getGarbageCollectorMXBeans();
	int gcBeanSize=gcBeanList.size();
	String gcNames[]=new String[gcBeanSize];
	long gcCount[]=new long[gcBeanSize];
	double gcTime[]=new double[gcBeanSize];
	int gcLoop=0;
	for(GarbageCollectorMXBean tempGCBean:gcBeanList) {
		gcNames[gcLoop]=tempGCBean.getName();
		gcCount[gcLoop]=tempGCBean.getCollectionCount();
		gcTime[gcLoop]=tempGCBean.getCollectionTime()/1000.0;
		gcLoop++;
	}
	
	/********************* get Stack Traces infos start *******************/
	// Because tempThreadInfo.getStackTrace() don't return anything, I use this way
	if(viewLockInfo) {
		Map<Thread,StackTraceElement[]> stackInfos=null;
		stackInfos=Thread.getAllStackTraces();

		for (Thread  tempThread: stackInfos.keySet()) {
		    stackTraceMap.put(tempThread.getId(),stackInfos.get(tempThread));
	    }
	}
	/********************* get Stack Traces infos end *******************/
	try {
		boolean blockFlag=false;
		String threadName = "";
		State threadState = null;
		ThreadInfo tempThreadInfo = null;
		long tempThreadTime = 0;

		for(long id : threadList) {
			blockFlag=false;
			printDataString.append("<TR>");
			tempThreadTime=tmxBean.getThreadCpuTime(id);
			tempThreadInfo=tmxBean.getThreadInfo(id);
			
			threadName=tempThreadInfo.getThreadName();
			threadState=tempThreadInfo.getThreadState();//.getLockedSynchronizers();
			if(threadState==State.BLOCKED) {blocked++;
				blockFlag=true;
			} else if(threadState==State.NEW) {newthread++;
			} else if(threadState==State.RUNNABLE) {runnable++;
			} else if(threadState==State.TERMINATED) {terminated++;
			} else if(threadState==State.TIMED_WAITING) {timed_waiting++;
			} else if(threadState==State.WAITING){ waiting++;
			}

			//MonitorInfo[] mis=tempThreadInfo.getLockedMonitors();
			int tempOrdinal=threadState.ordinal();
			if(viewAllThreadInfo) {
				//sb.append("<BR>Ordinal="+tempOrdinal+" "+threadName+"=>"+threadState +" ThreadTime="+tempThreadTime);
				printDataString.append("<TD  align=\"center\" bgcolor=\"#FFFFFF\" class=\"txt\">");
				printDataString.append(tempOrdinal);
				printDataString.append("</TD><TD bgcolor=\"#FFFFFF\" class=txt id=\"")
						.append(threadName)
						.append("\">"); 

				if((tempThreadTime/1000000000) > 200){
					if(threadState==State.RUNNABLE){
						printDataString.append("<font size=12 color=red>"+threadName+"</font>");
					}else{
						printDataString.append(threadName);
					}
				}else{
					printDataString.append(threadName);
				}
				
				printDataString.append("</TD>");
				
				if(blockFlag) {
					printDataString.append("<TD bgcolor=\"red\" class=\"txt\" >");
					printDataString.append("<B><FONT color=white>");
					printDataString.append(threadState);
					printDataString.append("</FONT></B>");
				} else {
					printDataString.append("<TD bgcolor=\"#FFFFFF\" class=\"txt\" >");
					printDataString.append(threadState);
				}
				printDataString.append("</TD><TD align=right  bgcolor=\"#FFFFFF\" class=\"txt\" >");
				printDataString.append(getTimeString(tempThreadTime));
				printDataString.append("</TD>");
				if(viewLockInfo) {
					printDataString.append("<TD  bgcolor=\"#FFFFFF\" class=\"txt\" >");
					String lockOwnerName=tempThreadInfo.getLockOwnerName();
					if(lockOwnerName!=null) {
						printDataString.append(lockOwnerName);
						lockOwnerSet.add(lockOwnerName);
						
					} else {
						printDataString.append("&nbsp;");
					}
					printDataString.append("</TD>");
					
					printDataString.append("<TD  bgcolor=\"#FFFFFF\" class=\"txt\" >");
					StackTraceElement[] tempStackTraceElement =stackTraceMap.get(id);
					int stackTraceSize=tempStackTraceElement.length;
					if(stackTraceSize!=0) {
						if(stackTraceSize<=stackTraceNumber) {
							for (StackTraceElement line: tempStackTraceElement) {
								printDataString.append(line).append("<BR>");
							}
						} else{
							for(int loop=0;loop<stackTraceNumber;loop++) {
								printDataString.append(tempStackTraceElement[loop]).append("<BR>");
							}
						}
					} else {
						printDataString.append("&nbsp;");
					}
					printDataString.append("</TD>");
				} 
			}
			printDataString.append("</TR>\n");
		}
		printDataString.append("</TABLE>");
		out.flush();
		//Thread.sleep(periodMillis);
		
	} catch(Exception e) {
		e.printStackTrace();
	}

	MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
	MemoryUsage heapMem=mbean.getHeapMemoryUsage();
	MemoryUsage nonHeapMem=mbean.getNonHeapMemoryUsage();
	String heapInit=getMemoryString(heapMem.getInit());
	String heapUsed=getMemoryString(heapMem.getUsed());
	String heapMax=getMemoryString(heapMem.getMax());
	String heapCommitted=getMemoryString(heapMem.getCommitted());
/*************************** Data Collect end *******************/
%>

<!-------------------------- Data View Start -------------------->

<span class="style2">
<span class="style5">*</span>GC 및 WAS 메모리 현황
</span><BR>

<table width="<%=pageWidth %>" border="0" cellpadding="5" cellspacing="1" bgcolor="#003399">
<% for(int loop=0;loop<gcBeanSize;loop++) { %>
<tr>
	<td width="50" bgcolor="#E0E0E0" class="txt">GC Name</td>
	<td width="150" bgcolor="#FFFFFF" class="txt"><%=gcNames[loop]%></td>
	<td  width="50" bgcolor="#E0E0E0" class="txt">GC Count</td>
	<td width="100" bgcolor="#FFFFFF" class="txt"><%=gcCount[loop]%></td> 
	<td width="50" bgcolor="#E0E0E0" class="txt">GC Time</td>
	<td width="150" bgcolor="#FFFFFF" class="txt"><%=gcTime[loop] %> sec</td>
	<td width="130" bgcolor="#E0E0E0" class="txt">Average GC Time</td>
	<td width="150" bgcolor="#FFFFFF" class="txt"><%=gcTime[loop]/gcCount[loop] * 1000 %> ms </td>
	
</tr>
<% } %>
</table>

<BR>
<table width="<%=pageWidth %>" border="0" cellpadding="5" cellspacing="1" bgcolor="#003399">
<tr>
	<td width="100" bgcolor="#E0E0E0" class="txt">Heap Used Mem</td>
	<td width="90" bgcolor="#FFFFFF" class="txt"><%=heapUsed%></td>
	<td  width="100" bgcolor="#E0E0E0" class="txt">Heap Min Mem</td>
	<td width="90" bgcolor="#FFFFFF" class="txt"><%=heapInit%></td> 
	<td width="100" bgcolor="#E0E0E0" class="txt">Heap Max Mem</td>
	<td width="90" bgcolor="#FFFFFF" class="txt"><%=heapMax %></td>
	<td width="100" bgcolor="#E0E0E0" class="txt">Heap Committed</td>
	<td width="90" bgcolor="#FFFFFF" class="txt"><%= heapCommitted %> </td>	
</tr>
</table>

<Table width="1024" border="0" cellpadding="5" cellspacing="1" bgcolor="#003399">
<tr align=center>
  <td bgcolor="#E0E0E0" class="txt">Heap Memory 이름</td>
  <td bgcolor="#E0E0E0" width="200" class="txt">Memory Manager Names</td>
  <td bgcolor="#E0E0E0" width="80" class="txt">사용율(%)</td>
  <td bgcolor="#E0E0E0" class="txt">상세 사용량</td>
</tr>

<%

/********************* get MemoryPoolMXBeans info ********************************/
	List<MemoryPoolMXBean> mempoolsmbeans = ManagementFactory.getMemoryPoolMXBeans();
	long commitMem = 0;
	long useMem= 0;

	for( MemoryPoolMXBean mempoolmbean : mempoolsmbeans )
	{
		if(mempoolmbean.getType().toString().equals("Heap memory")){// Heap 메모리 영역만을 출력
			StringBuffer manegerName = new StringBuffer();
			StringBuffer usageDesc = new StringBuffer();

			out.println( "<tr>");
			out.println( "<td bgcolor='#FFFFFF' class='txt'>"+mempoolmbean.getName()+"</td>");

			String[] memManagerNames = mempoolmbean.getMemoryManagerNames();
			for( int i=0; i < memManagerNames.length ; i++)
			{
			  manegerName.append(memManagerNames[ i ]+"<br>");
			}
			out.println( "<td bgcolor='#FFFFFF' class='txt'>"+manegerName.toString()+"</td>");
			


			commitMem = mempoolmbean.getUsage().getCommitted() ;
			useMem = mempoolmbean.getUsage().getUsed() ;
			
			if(mempoolmbean.getName().endsWith("Old Gen")){
				if(((useMem*100/commitMem)) > memLimitRate){
					out.println( "<td bgcolor='#FF0000' class='txt'>"+((useMem*100/commitMem))+" %</td>");
				}else{
					out.println( "<td bgcolor='#FFFFFF' class='txt'>"+((useMem*100/commitMem))+" %</td>");
				}
				
			}else{
				out.println( "<td bgcolor='#FFFFFF' class='txt'>"+((useMem*100/commitMem))+" %</td>");
			}
			
			
			usageDesc.append("<pre>초기할당량="+getMemoryString(mempoolmbean.getUsage().getInit())+"&nbsp; ,");
			usageDesc.append("현재사용량="+getMemoryString(mempoolmbean.getUsage().getUsed())+"&nbsp; ,");
			usageDesc.append("최대할당량="+getMemoryString(mempoolmbean.getUsage().getMax())+"&nbsp; ,");
			usageDesc.append("최대허용량="+getMemoryString(mempoolmbean.getUsage().getCommitted())+"</pre>");
			
			out.println( "<td bgcolor='#FFFFFF' class='txt'>"+usageDesc.toString()+"</td>");

			/**out.println( "<br>Collection Usage<br>");

			out.println("초기값 : "+getMemoryString(mempoolmbean.getCollectionUsage().getInit()));
			out.println("사용량 : "+getMemoryString(mempoolmbean.getCollectionUsage().getUsed()));
			out.println("최대사용량 : "+getMemoryString(mempoolmbean.getCollectionUsage().getMax()));
			out.println("최대허용값 : "+getMemoryString(mempoolmbean.getCollectionUsage().getCommitted()));**/


			/**out.println("<br>Peak Usage <br>");
			out.println("초기값 : "+getMemoryString(mempoolmbean.getPeakUsage().getInit()));
			out.println("사용량 : "+getMemoryString(mempoolmbean.getPeakUsage().getUsed()));
			out.println("최대사용량 : "+getMemoryString(mempoolmbean.getPeakUsage().getMax()));
			out.println("최대허용값 : "+getMemoryString(mempoolmbean.getPeakUsage().getCommitted()));**/
			
		}
	}
%>


 
  
</tr>
</table>

<BR>

<span class="style2">
<span class="style5">*</span>쓰레드 현황 요약
</span><BR>

<table width="<%=pageWidth %>" border="0" cellpadding="5" cellspacing="1" bgcolor="#003399">
<tr>
	<td width="163" bgcolor="#E0E0E0" class="txt">Total Started Thread Count</td>
	<td width="55" bgcolor="#FFFFFF" class="txt"><%=tmxBean.getTotalStartedThreadCount()%></td> 
	<td width="149" bgcolor="#E0E0E0" class="txt">Peak Thread Count</td>
	<td width="41" bgcolor="#FFFFFF" class="txt"><%=tmxBean.getPeakThreadCount()%></td>
	<td width="149" bgcolor="#E0E0E0" class="txt">Current Thread Count</td>
	<td width="41" bgcolor="#FFFFFF" class="txt"><%=tmxBean.getThreadCount() %></td>
	<td width="149" bgcolor="#E0E0E0" class="txt">Daemon Thread Count</td>
	<td width="41" bgcolor="#FFFFFF" class="txt"><%= tmxBean.getDaemonThreadCount() %> </td>	
</tr>
</table>

<BR>

<Table width="<%=pageWidth %>" border="0" cellpadding="5" cellspacing="1" bgcolor="#003399">
<tr align=center>
  <td bgcolor="#E0E0E0" class="txt">쓰레드 상태</td>
  <td bgcolor="#E0E0E0" width="120" class="txt">상태별 쓰레드 갯수</td>
  <td bgcolor="#E0E0E0" class="txt">상세 설명</td>
</tr>
<tr>
  <td bgcolor="#FFFFFF" class="txt">NEW</td>
  <td bgcolor="#FFFFFF" class="txt"><%=newthread %></td>
  <td bgcolor="#FFFFFF" class="txt">A thread that has not yet started is in this state. </td>
</tr><tr>
  <td bgcolor="#FFFFFF" class="txt">RUNNABLE</td>
  <td bgcolor="#FFFFFF" class="txt"><%=runnable%></td>
  <td bgcolor="#FFFFFF" class="txt">A thread executing in the Java virtual machine is in this state. </td>
</tr><tr>
  <td bgcolor="#FFFFFF" class="txt"><B> BLOCKED </B></td>
  <td bgcolor="#FFFFFF" class="txt"><%=blocked%></td>
  <td bgcolor="#FFFFFF" class="txt"><font color=red >A thread that is blocked waiting for a monitor lock is in this state.</font> </td>
</tr><tr>
  <td bgcolor="#FFFFFF" class="txt">WAITING</td>
  <td bgcolor="#FFFFFF" class="txt"><%=waiting%></td>
  <td bgcolor="#FFFFFF" class="txt">A thread that is waiting indefinitely for another thread to perform a particular action is in this state. </td>
</tr><tr>
  <td bgcolor="#FFFFFF" class="txt">TIMED_WAITING</td>
  <td bgcolor="#FFFFFF" class="txt"><%=timed_waiting%></td>
  <td bgcolor="#FFFFFF" class="txt">A thread that is waiting for another thread to perform an action for up to a specified waiting time is in this state. </td>
</tr><tr>
  <td bgcolor="#FFFFFF" class="txt">TERMINATED</td>
  <td bgcolor="#FFFFFF" class="txt"><%=terminated%></td>
  <td bgcolor="#FFFFFF" class="txt">A thread that has exited is in this state.</td>
</tr>
</table>
<BR>
<%
	if(viewAllThreadInfo) {
		if(lockOwnerSet.size()!=0) {
			out.println("<span class=\"style2\"><span class=\"style5\">*</span> Current Lock Owner List</span><BR>");
			out.println("<TABLE><TR>");
			for(String tempOwnerName:lockOwnerSet)  {
				out.println("<td bgcolor=\"#FFFFFF\" class=\"txt\"><font color=blue><B>&nbsp;"+tempOwnerName+"</B></font>&nbsp;</TD>");
			}
			out.println("</TR></TABLE><BR>");
		}
%>
		
		<span class="style2"><span class="style5">*</span>전체 쓰레드 정보</span><BR>
<%
		out.println(printDataString.toString());

	}
%>

<!-------------------------- Data View end -------------------->
	
<Script>
<%
if(viewAllThreadInfo) {
	for(String tempOwnerName:lockOwnerSet)  {
		out.println("document.getElementById('"+tempOwnerName+"').className='txtx';");
	}
}
%>
</Script>
<BR><BR>



</BODY>
</HTML>

