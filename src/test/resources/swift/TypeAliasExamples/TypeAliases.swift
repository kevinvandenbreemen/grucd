protocol SomethingWithAliasesStrategy {
    typealias FetchUserCompletion = (User?, Error?) -> Void

    var someField: SomeType { get }

    func fetchUser(completion: @escaping FetchUserCompletion)
}