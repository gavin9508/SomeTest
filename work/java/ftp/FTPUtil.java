package com.sheshu.common.ftp;

import com.sheshu.common.configer.FTPClientConfig;
import com.sheshu.common.ftp.factory.FTPClientFactory;
import com.sheshu.common.ftp.pool.FTPClientPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * 因为之前使用连接池的工具类有问题
 * 导致方法混乱，不堪大用。今天解决了其BUG 故重新做一个整理。
 * 之后的FTP统一用这个
 *
 * @author:gavin
 * @date:2019/7/5 10 26
 * @Description
 **/
@Slf4j
@Component
public class FTPUtil {
    /**
     * FTP的连接池
     */
    @Autowired
    public static FTPClientPool ftpClientPool;
    /**
     * FTPClient对象
     */
    public static FTPClient ftpClient;

    private static FTPUtil ftpUtil;

    @Autowired
    private FTPClientConfig ftpClientConfig;

    /**
     * 初始化设置
     *
     * @return
     */
    @PostConstruct
    public boolean init() {
        FTPClientFactory factory = new FTPClientFactory(ftpClientConfig);
        ftpUtil = this;
        try {
            ftpClientPool = new FTPClientPool(factory);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 主要修改了这里
     * 对象使用完毕后还回池子要做销毁
     * 很重要
     *
     * @auther: gavin
     * @date: 2019/7/5 11:04
     * @param:
     * @return:
     */
    public static void getFTPClient() throws Exception {
        //初始化的时候从队列中取出一个连接
        synchronized (ftpClientPool) {
            ftpClient = ftpClientPool.borrowObject();
            //被动模式
            ftpClient.enterLocalPassiveMode();
        }
    }


    /**
     * 当前命令执行完成命令完成
     *
     * @throws IOException
     */
    public void complete() throws IOException {
        ftpClient.completePendingCommand();
    }

    /**
     * 当前线程任务处理完成，加入到队列的最后
     *
     * @return
     */
    public static void disconnect() throws Exception {
        ftpClientPool.addObject(ftpClient);
    }

    /**
     * 退出
     *
     * @auther: gavin
     * @date: 2019/7/5 11:47
     * @param: [input]
     * @return: void
     */
    private static void destroy(InputStream input) {
        if (null != input) {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                FTPUtil.disconnect();
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Description: 向FTP服务器上传文件
     *
     * @param remoteFile 上传到FTP服务器上的文件名
     * @param input      本地文件流
     * @return 成功返回true，否则返回false
     * @Version1.0
     */
    public static boolean uploadFile(String remoteFile, InputStream input) {
        boolean result = false;
        try {
            getFTPClient();
            ftpClient.enterLocalPassiveMode();
            result = ftpClient.storeFile(remoteFile, input);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            destroy(input);
        }
        return result;
    }

    /**
     * 上传文件
     *
     * @auther: gavin
     * @date: 2019/1/21 14:41
     * @param: [path, remoteFile, input]
     * @return: boolean
     */
    public static boolean uploadByPath(String filePath, String remoteFile, InputStream input) {
        boolean result = false;
        try {
            getFTPClient();
            //1.创建文件夹
            ftpClient.makeDirectory(filePath);
            //2.进入文件夹
            ftpClient.changeWorkingDirectory(filePath);
            //3.上传文件
            result = ftpClient.storeFile(remoteFile, input);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            destroy(input);
        }
        return result;
    }

    /**
     * 从ftp中获取文件流
     *
     * @param filePath
     * @return
     * @throws Exception
     */
    public static InputStream getInputStream(String filePath, String fileName) throws Exception {
        getFTPClient();
        ftpClient.changeWorkingDirectory(filePath);
        InputStream inputStream = ftpClient.retrieveFileStream(fileName);
        return inputStream;
    }
}
