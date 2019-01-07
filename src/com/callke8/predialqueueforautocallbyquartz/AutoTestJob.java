package com.callke8.predialqueueforautocallbyquartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class AutoTestJob implements Job {

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		System.out.println("开始时间毫秒:" + System.currentTimeMillis());
		System.out.println("我测试打印数据....");
	}

}
