package com.sheshu.common.ftp;

import com.sheshu.common.configer.FTPClientConfig;
import com.sheshu.common.ftp.factory.FTPClientFactory;
import com.sheshu.common.ftp.pool.FTPClientPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.MalformedURLException;
import java.util.StringTokenizer;

/**
 * @Auther: hrabbit
 * @Date: 2018-12-03 3:47 PM
 * @Description:
 */
@Slf4j
@Component
public class FTPUtils {

    /**
     * FTP的连接池
     */
    @Autowired
    public static FTPClientPool ftpClientPool;
    /**
     * FTPClient对象
     */
    public static FTPClient ftpClient;


    private static FTPUtils ftpUtils;

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
        ftpUtils = this;
        try {
            ftpClientPool = new FTPClientPool(factory);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 初始化ftp服务器
     */
    public static void initFtpClient(String hostname, String username, String password, Integer port) {
        ftpClient = new FTPClient();
        ftpClient.setControlEncoding("utf-8");
        try {
            //连接ftp服务器
            ftpClient.connect(hostname, port);
            //登录ftp服务器
            ftpClient.login(username, password);
            //是否成功登录服务器
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                System.out.println("ftp服务器登录成功");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取连接对象
     *
     * @return
     * @throws Exception
     */
    public static void getFTPClient() throws Exception {
        //初始化的时候从队列中取出一个连接
        if (ftpClient == null) {
            synchronized (ftpClientPool) {
                ftpClient = ftpClientPool.borrowObject();
            }
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
            input.close();
            FTPUtils.disconnect();
            ftpClient.logout();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 上传文件
     *
     * @param pathname    ftp服务保存地址
     * @param fileName    上传到ftp的文件名
     * @param inputStream 输入文件流
     * @return
     */
    public static boolean uploadFile(String hostname, String username, String password, Integer port, String pathname, String fileName, InputStream inputStream) {
        boolean flag = false;
        try {
            System.out.println("开始上传文件");
            initFtpClient(hostname, username, password, port);
            ftpClient.setControlEncoding("utf-8");
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            CreateDirecroty(pathname);
            ftpClient.makeDirectory(pathname);
            ftpClient.setControlEncoding("utf-8");
            ftpClient.storeFile(fileName, inputStream);
            System.out.println(fileName + "上传结束");
            inputStream.close();
            ftpClient.logout();
            flag = true;
            System.out.println("上传文件成功");

        } catch (Exception e) {
            System.out.println("上传文件失败");
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }


    //创建多层目录文件，如果有ftp服务器已存在该文件，则不创建，如果无，则创建
    public static boolean CreateDirecroty(String remote) throws IOException {
        boolean success = true;
        String directory = remote + "/";
        // 如果远程目录不存在，则递归创建远程服务器目录
        if (!directory.equalsIgnoreCase("/") && !changeWorkingDirectory(new String(directory))) {
            int start = 0;
            int end = 0;
            if (directory.startsWith("/")) {
                start = 1;
            } else {
                start = 0;
            }
            end = directory.indexOf("/", start);
            String path = "";
            String paths = "";
            while (true) {
                String subDirectory = new String(remote.substring(start, end).getBytes("UTF-8"), "iso-8859-1");
                path = path + "/" + subDirectory;
                if (!existFile(path)) {
                    if (makeDirectory(subDirectory)) {
                        changeWorkingDirectory(subDirectory);
                    } else {
                        System.out.println("创建目录[" + subDirectory + "]失败");
                        changeWorkingDirectory(subDirectory);
                    }
                } else {
                    changeWorkingDirectory(subDirectory);
                }

                paths = paths + "/" + subDirectory;
                start = end + 1;
                end = directory.indexOf("/", start);
                // 检查所有目录是否创建完毕
                if (end <= start) {
                    break;
                }
            }
        }
        return success;
    }

    //判断ftp服务器文件是否存在
    public static boolean existFile(String path) throws IOException {
        boolean flag = false;
        FTPFile[] ftpFileArr = ftpClient.listFiles(path);
        if (ftpFileArr.length > 0) {
            flag = true;
        }
        return flag;
    }

    //改变目录路径
    public static boolean changeWorkingDirectory(String directory) {
        boolean flag = true;
        try {
            flag = ftpClient.changeWorkingDirectory(directory);
            if (flag) {
                System.out.println("进入文件夹" + directory + " 成功！");
            } else {
                System.out.println("进入文件夹" + directory + " 失败！开始创建文件夹");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return flag;
    }

    /**
     * Description: 向FTP服务器上传文件
     *
     * @param remoteFile 上传到FTP服务器上的文件名
     * @param localFile  本地文件
     * @return 成功返回true，否则返回false
     * @Version1.0
     */
    public static boolean uploadFile(String remoteFile, String localFile) {
        FileInputStream input = null;
        try {
            input = new FileInputStream(new File(localFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return uploadFile(remoteFile, input);
    }

    /**
     * 拷贝文件
     *
     * @param fromFile
     * @param toFile
     * @return
     * @throws Exception
     */
    public boolean copyFile(String fromFile, String toFile) throws Exception {
        InputStream in = getFileInputStream(fromFile);
        getFTPClient();
        boolean flag = ftpClient.storeFile(toFile, in);
        in.close();
        return flag;
    }

    /**
     * 获取文件输入流
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static InputStream getFileInputStream(String fileName) throws Exception {
        ByteArrayOutputStream fos = new ByteArrayOutputStream();
        getFTPClient();
        ftpClient.retrieveFile(fileName, fos);
        ByteArrayInputStream in = new ByteArrayInputStream(fos.toByteArray());
        fos.close();
        return in;
    }

    /**
     * Description: 从FTP服务器下载文件
     *
     * @return
     * @Version1.0
     */
    public static boolean downFile(String remoteFile, String localFile) {
        boolean result = false;
        try {
            getFTPClient();
            OutputStream os = new FileOutputStream(localFile);
            ftpClient.retrieveFile(remoteFile, os);
            ftpClient.logout();
            ftpClient.disconnect();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
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
    public static InputStream getInputStream(String filePath) throws Exception {
        getFTPClient();
        InputStream inputStream = ftpClient.retrieveFileStream(filePath);
        return inputStream;
    }

    /**
     * ftp中文件重命名
     *
     * @param fromFile
     * @param toFile
     * @return
     * @throws Exception
     */
    public boolean rename(String fromFile, String toFile) throws Exception {
        getFTPClient();
        boolean result = ftpClient.rename(fromFile, toFile);
        return result;
    }

    /**
     * 获取ftp目录下的所有文件
     *
     * @param dir
     * @return
     */
    public FTPFile[] getFiles(String dir) throws Exception {
        getFTPClient();
        FTPFile[] files = new FTPFile[0];
        try {
            files = ftpClient.listFiles(dir);
        } catch (Throwable thr) {
            thr.printStackTrace();
        }
        return files;
    }

    /**
     * 获取ftp目录下的某种类型的文件
     *
     * @param dir
     * @param filter
     * @return
     */
    public FTPFile[] getFiles(String dir, FTPFileFilter filter) throws Exception {
        getFTPClient();
        FTPFile[] files = new FTPFile[0];
        try {
            files = ftpClient.listFiles(dir, filter);
        } catch (Throwable thr) {
            thr.printStackTrace();
        }
        return files;
    }

    //创建目录
    public static boolean makeDirectory(String dir) {
        boolean flag = true;
        try {
            flag = ftpClient.makeDirectory(dir);
            if (flag) {
                System.out.println("创建文件夹" + dir + " 成功！");

            } else {
                System.out.println("创建文件夹" + dir + " 失败！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public static boolean mkdirs(String dir) throws Exception {
        boolean result = false;
        if (null == dir) {
            return result;
        }
        getFTPClient();
        ftpClient.changeWorkingDirectory("/");
        StringTokenizer dirs = new StringTokenizer(dir, "/");
        String temp = null;
        while (dirs.hasMoreElements()) {
            temp = dirs.nextElement().toString();
            //创建目录
            ftpClient.makeDirectory(temp);
            //进入目录
            ftpClient.changeWorkingDirectory(temp);
            result = true;
        }
        ftpClient.changeWorkingDirectory("/");
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
    public static boolean uploadByPath(String path, String remoteFile, InputStream input) {
        boolean result = false;
        try {
            getFTPClient();
            ftpClient.enterLocalPassiveMode();
            //1.创建文件夹
            makeDir(path);
            //2.上传文件
            result = ftpClient.storeFile(remoteFile, input);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (ftpClient.isConnected()) {
                try {
                    FTPUtils.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private static boolean makeDir(String dir) throws Exception {
        boolean result = false;
        if (null == dir) {
            return result;
        }
        ftpClient.changeWorkingDirectory("/");
        StringTokenizer dirs = new StringTokenizer(dir, "/");
        String temp = null;
        while (dirs.hasMoreElements()) {
            temp = dirs.nextElement().toString();
            //创建目录
            ftpClient.makeDirectory(temp);
            //进入目录
            ftpClient.changeWorkingDirectory(temp);
            result = true;
        }
        return result;
    }
}
