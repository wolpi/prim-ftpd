package org.primftpd.pojo;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class LsOutputParserTests {

    private InputStream stream(String filename) throws IOException {
        return getClass().getResourceAsStream("/ls-output/" + filename);
    }

    @Test
    public void notExists() {
        LsOutputBean bean = new LsOutputBean("name");
        Assert.assertFalse(bean.isExists());
    }

    @Test
    public void noSuchFile() throws IOException {
        List<LsOutputBean> beans = new LsOutputParser().parse(stream("no-such-file.txt"));
        Assert.assertTrue(beans.isEmpty());
    }

    @Test
    public void good() throws IOException {
        List<LsOutputBean> beans = new LsOutputParser().parse(stream("good.txt"));
        Assert.assertEquals(3, beans.size());

        LsOutputBean dir = beans.get(0);
        Assert.assertTrue(dir.isExists());
        Assert.assertTrue(dir.isDir());
        Assert.assertFalse(dir.isFile());
        Assert.assertFalse(dir.isLink());
        Assert.assertTrue(dir.isUserReadable());
        Assert.assertTrue(dir.isUserWritable());
        Assert.assertTrue(dir.isUserExecutable());
        Assert.assertTrue(dir.isGroupReadable());
        Assert.assertFalse(dir.isGroupWritable());
        Assert.assertTrue(dir.isGroupExecutable());
        Assert.assertTrue(dir.isOtherReadable());
        Assert.assertFalse(dir.isOtherWritable());
        Assert.assertTrue(dir.isOtherExecutable());
        Assert.assertFalse(dir.isHasAcl());
        Assert.assertEquals(1, dir.getLinkCount());
        Assert.assertEquals("user", dir.getUser());
        Assert.assertEquals("group", dir.getGroup());
        Assert.assertEquals(1234, dir.getSize());
        Assert.assertEquals("dir_name", dir.getName());
        Assert.assertNull(dir.getLinkTarget());
        Assert.assertNotNull(dir.getDate());
        Assert.assertEquals(88200000, dir.getDate().getTime());

        LsOutputBean file1 = beans.get(1);
        Assert.assertTrue(file1.isFile());
        Assert.assertFalse(file1.isDir());
        Assert.assertFalse(file1.isLink());
        Assert.assertEquals("file_name", file1.getName());
        Assert.assertNotNull(file1.getDate());
        Assert.assertEquals(86400000, file1.getDate().getTime());

        LsOutputBean file2 = beans.get(2);
        Assert.assertTrue(file2.isFile());
        Assert.assertTrue(file2.isHasAcl());
        Assert.assertEquals("file_2", file2.getName());
        Assert.assertNotNull(file2.getDate());
        long expectedTime =
                LsOutputParser.CURRENT_YEAR_MILLIS +
                        (24 * 60 * 60 * 1000) +
                        (30 * 60 * 1000);
        Assert.assertEquals(expectedTime, file2.getDate().getTime());
    }

    @Test
    public void symLink() throws IOException {
        List<LsOutputBean> beans = new LsOutputParser().parse(stream("sym-link.txt"));
        Assert.assertEquals(1, beans.size());

        LsOutputBean link = beans.get(0);
        Assert.assertFalse(link.isDir());
        Assert.assertFalse(link.isFile());
        Assert.assertTrue(link.isLink());
        Assert.assertEquals("link_name", link.getName());
        Assert.assertEquals("/absolute/link/target", link.getLinkTarget());
        Assert.assertNotNull(link.getDate());
        Assert.assertTrue(link.getDate().getTime() > 0);
    }

    @Test
    public void nameWithSpaces() throws IOException {
        List<LsOutputBean> beans = new LsOutputParser().parse(stream("name-with-spaces.txt"));
        Assert.assertEquals(4, beans.size());

        Assert.assertEquals("dir name", beans.get(0).getName());
        Assert.assertEquals("file name", beans.get(1).getName());
        Assert.assertEquals("file 2", beans.get(2).getName());
        Assert.assertEquals("link name", beans.get(3).getName());
        Assert.assertEquals("/absolute/link target", beans.get(3).getLinkTarget());
    }

    @Test
    public void noLinkCount() throws IOException {
        List<LsOutputBean> beans = new LsOutputParser().parse(stream("no-link-count.txt"));
        Assert.assertEquals(3, beans.size());

        LsOutputBean dir = beans.get(0);
        Assert.assertTrue(dir.isExists());
        Assert.assertTrue(dir.isDir());
        Assert.assertFalse(dir.isFile());
        Assert.assertFalse(dir.isLink());
        Assert.assertTrue(dir.isUserReadable());
        Assert.assertTrue(dir.isUserWritable());
        Assert.assertTrue(dir.isUserExecutable());
        Assert.assertTrue(dir.isGroupReadable());
        Assert.assertFalse(dir.isGroupWritable());
        Assert.assertTrue(dir.isGroupExecutable());
        Assert.assertTrue(dir.isOtherReadable());
        Assert.assertFalse(dir.isOtherWritable());
        Assert.assertTrue(dir.isOtherExecutable());
        Assert.assertFalse(dir.isHasAcl());
        Assert.assertEquals(0, dir.getLinkCount());
        Assert.assertEquals("user", dir.getUser());
        Assert.assertEquals("group", dir.getGroup());
        Assert.assertEquals(1234, dir.getSize());
        Assert.assertEquals("dir_name", dir.getName());
        Assert.assertNull(dir.getLinkTarget());
        Assert.assertNotNull(dir.getDate());
        Assert.assertEquals(88200000, dir.getDate().getTime());

        LsOutputBean file1 = beans.get(1);
        Assert.assertTrue(file1.isFile());
        Assert.assertFalse(file1.isDir());
        Assert.assertFalse(file1.isLink());
        Assert.assertEquals("file_name", file1.getName());
        Assert.assertNotNull(file1.getDate());
        Assert.assertEquals(86400000, file1.getDate().getTime());
        Assert.assertEquals(0, file1.getLinkCount());

        LsOutputBean file2 = beans.get(2);
        Assert.assertTrue(file2.isFile());
        Assert.assertTrue(file2.isHasAcl());
        Assert.assertEquals("file_2", file2.getName());
        Assert.assertNotNull(file2.getDate());
        long expectedTime =
                LsOutputParser.CURRENT_YEAR_MILLIS +
                        (24 * 60 * 60 * 1000) +
                        (30 * 60 * 1000);
        Assert.assertEquals(expectedTime, file2.getDate().getTime());
        Assert.assertEquals(0, file2.getLinkCount());

    }
}
