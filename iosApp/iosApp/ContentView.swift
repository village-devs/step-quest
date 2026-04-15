import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var viewModel = StepQuestViewModel()
    
    var body: some View {
        GameScreenView(viewModel: viewModel)
    }
}

class StepQuestViewModel: ObservableObject {
    private let gameViewModel: GameViewModel
    
    init() {
        let stepCounter = PlatformStepCounter()
        let configProvider = ResourceConfigProvider()
        let progressStorage = createProgressStorage()
        let gameRepository = GameRepositoryImpl(configProvider: configProvider, progressStorage: progressStorage)
        
        // Config will be loaded asynchronously
        self.gameViewModel = GameViewModelImpl(
            gameRepository: gameRepository,
            mapService: MapServiceImpl(config: GameConfig(startNodeId: "", endNodeId: "", nodes: [], defaultStepsToNextChoice: 100)),
            stepCounter: stepCounter
        )
        
        gameViewModel.start()
    }
    
    var state: GameState {
        gameViewModel.state.value
    }
    
    func makeChoice(choiceId: String) {
        gameViewModel.makeChoice(choiceId: choiceId)
    }
    
    func dismissChoiceModal() {
        gameViewModel.dismissChoiceModal()
    }
    
    func resetGame() {
        gameViewModel.resetGame()
    }
}

private func createProgressStorage() -> ProgressStorage {
    // iOS implementation using UserDefaults or SQLite
    return IOSProgressStorage()
}

class IOSProgressStorage: ProgressStorage {
    private let defaults = UserDefaults.standard
    private let jsonEncoder = JSONEncoder()
    private let jsonDecoder = JSONDecoder()
    
    func save(progress: PlayerProgress) async throws {
        if let data = try? jsonEncoder.encode(progress) {
            defaults.set(data, forKey: "playerProgress")
        }
    }
    
    func get() async throws -> PlayerProgress? {
        guard let data = defaults.data(forKey: "playerProgress") else { return nil }
        return try? jsonDecoder.decode(PlayerProgress.self, from: data)
    }
    
    func clear() async throws {
        defaults.removeObject(forKey: "playerProgress")
    }
}

struct GameScreenView: View {
    @ObservedObject var viewModel: StepQuestViewModel
    @State private var showingModal = false
    
    var body: some View {
        ZStack {
            Color(.systemBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 20) {
                // Header
                Text("StepQuest")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.gray)
                
                // Current location card
                VStack(alignment: .leading, spacing: 10) {
                    if let node = viewModel.state.currentNode {
                        Text(node.name)
                            .font(.title2)
                            .fontWeight(.semibold)
                        Text(node.description)
                            .font(.body)
                            .foregroundColor(.gray)
                    }
                    
                    if viewModel.state.isAtEnd {
                        Text("🎉 Путь завершён!")
                            .font(.title3)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(16)
                
                // Virtual map placeholder
                VStack {
                    ZStack {
                        Circle()
                            .fill(Color.blue.opacity(0.2))
                            .frame(width: 150, height: 150)
                        
                        Circle()
                            .fill(Color.blue)
                            .frame(width: 40, height: 40)
                    }
                    
                    Text("Виртуальная карта")
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color(.systemBlue).opacity(0.1))
                .cornerRadius(16)
                
                // Steps indicator
                HStack {
                    VStack(alignment: .leading) {
                        Text("Всего шагов")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Text("\(viewModel.state.totalSteps)")
                            .font(.title)
                            .fontWeight(.bold)
                    }
                    
                    Spacer()
                    
                    VStack {
                        Text("\(viewModel.state.stepsToNextChoice)")
                            .font(.title2)
                            .fontWeight(.bold)
                        Text("до выбора")
                            .font(.caption2)
                            .foregroundColor(.gray)
                    }
                }
                .padding()
                .background(Color(.systemOrange).opacity(0.1))
                .cornerRadius(16)
                
                // Reset button
                Button(action: { viewModel.resetGame() }) {
                    Text("Начать заново")
                        .fontWeight(.medium)
                }
                .buttonStyle(.borderedProminent)
                .tint(.red)
            }
            .padding()
        }
        .onChange(of: viewModel.state.showChoiceModal) { newValue in
            showingModal = newValue
        }
        .sheet(isPresented: $showingModal) {
            ChoiceModalView(
                choices: Array(viewModel.state.availableChoices),
                onChoiceSelected: { choiceId in
                    viewModel.makeChoice(choiceId: choiceId)
                }
            )
        }
    }
}

struct ChoiceModalView: View {
    let choices: [Choice]
    let onChoiceSelected: (String) -> Void
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("Развилка пути!")
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text("Выберите направление:")
                    .foregroundColor(.gray)
                
                ForEach(choices, id: \.id) { choice in
                    Button(action: {
                        onChoiceSelected(choice.id)
                        dismiss()
                    }) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(choice.text)
                                .fontWeight(.medium)
                            Text("\(choice.stepsRequired) шагов")
                                .font(.caption)
                                .opacity(0.8)
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                        .background(Color.green)
                        .cornerRadius(12)
                    }
                }
                
                Spacer()
            }
            .padding()
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
