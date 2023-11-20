# grucd

A library that generates an abstraction of a codebase that you can then use to create artifacts such as UML etc.

# Getting Started
You can extract an abstract model of your source code using the SourceCodeExtractor, like this:

```
        val extractor = SourceCodeExtractor()
        val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = "src/test/resources/")
        val model = extractor.buildModelWithFiles(fileNames)
```

Your source code is represented using a `Model` object.  This object contains a list of types along with any relations (inheritance, encapsulation, etc) GRUCD can detect between them.

# Contributing

