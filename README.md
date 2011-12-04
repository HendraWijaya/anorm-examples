This is a sample project using standalone Anorm 2.0-Beta from Play 2.0 framework. The models and test cases are copied from Yabe example in play-scala module. The magic extractor is extended to make sure Id and Pk type are working with Magic. I hope this will be fixed by the Play team in the future. 

Any reference to Id[T <: AnyVal] is also updated to use the corresponding Java type like java.lang.Int, java.lang.Long, etc because as of this writing Id[T <: AnyVal] is seen as Id[java.lang.Object] by Anorm.
