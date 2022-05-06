def checkFile(File file) {

    if (!file.isFile()) {

        throw new FileNotFoundException("file: " + file);
    }
}

checkFile(new File(basedir, "build.log"));
//checkFile(new File(basedir, "tsconfig.spec.json"));
//checkFile(new File(basedir, "package.json"));
//checkFile(new File(basedir, "target/dist/package.json"));