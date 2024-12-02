import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class RealTimeQuizApp {

    static int score = 0;
    static boolean answered = false;
    static String correctAnswer = "";
    static String[] options = new String[4];

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Gson gson = new Gson();

        System.out.println("--- Welcome to the Real-Time Quiz App ---");
        System.out.println("You will have 30 seconds to answer each question.");
        System.out.println("Difficulty levels: Easy -> Medium -> Hard -> Very Hard\n");

        while (true) {
            try {
                // Step 1: Get question from API
                String apiUrl = "https://opentdb.com/api.php?amount=1&type=multiple";
                JsonObject questionData = fetchQuestion(apiUrl, gson);

                if (questionData != null) {
                    String question = questionData.get("question").getAsString();
                    correctAnswer = questionData.get("answer").getAsString();
                    options = gson.fromJson(questionData.get("options").getAsJsonArray(), String[].class);

                    // Step 2: Display the question and options
                    System.out.println("\nQuestion: " + question);
                    for (int i = 0; i < options.length; i++) {
                        System.out.println((i + 1) + ". " + options[i]);
                    }

                    // Step 3: Start 30-second timer
                    answered = false;
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        int timeRemaining = 30;

                        @Override
                        public void run() {
                            if (timeRemaining > 0) {
                                System.out.print("\rTime left: " + timeRemaining + "s");
                                timeRemaining--;
                            } else {
                                if (!answered) {
                                    System.out.println("\nTime's up! Correct answer was: " + correctAnswer);
                                    answered = true;
                                    timer.cancel();
                                }
                            }
                        }
                    }, 0, 1000);

                    // Step 4: User input
                    System.out.print("\nEnter your answer (1-4): ");
                    if (sc.hasNextInt()) {
                        int userAnswer = sc.nextInt();
                        answered = true;
                        timer.cancel();

                        // Step 5: Check the answer
                        if (options[userAnswer - 1].equals(correctAnswer)) {
                            System.out.println("Correct!");
                            score++;
                        } else {
                            System.out.println("Wrong! The correct answer was: " + correctAnswer);
                        }
                    } else {
                        System.out.println("Invalid input! Correct answer was: " + correctAnswer);
                        sc.next(); // Clear invalid input
                    }

                    // Display the score
                    System.out.println("Your current score: " + score);
                }

                // Step 6: Ask if the user wants to continue
                System.out.print("\nDo you want to play again? (yes/no): ");
                String playAgain = sc.next();
                if (!playAgain.equalsIgnoreCase("yes")) {
                    break;
                }

                // Adding a slight delay (5 seconds) to prevent hitting API limit too frequently
                System.out.println("Waiting for next question...");
                Thread.sleep(5000); // 5 seconds delay to avoid hitting the rate limit

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        sc.close();
        System.out.println("\nThank you for playing! Your final score is: " + score);
    }

    // Function to fetch question data from API
    private static JsonObject fetchQuestion(String apiUrl, Gson gson) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
        if (jsonResponse.get("results").getAsJsonArray().size() > 0) {
            JsonObject questionData = jsonResponse.get("results").getAsJsonArray().get(0).getAsJsonObject();

            // Parse the question and options
            String question = questionData.get("question").getAsString();
            String correctAnswer = questionData.get("correct_answer").getAsString();
            String[] options = gson.fromJson(questionData.get("incorrect_answers").getAsJsonArray(), String[].class);

            // Add the correct answer to options (shuffled manually)
            String[] allOptions = new String[options.length + 1];
            System.arraycopy(options, 0, allOptions, 0, options.length);
            allOptions[options.length] = correctAnswer;

            JsonObject parsedData = new JsonObject();
            parsedData.addProperty("question", question);
            parsedData.add("options", gson.toJsonTree(allOptions));
            parsedData.addProperty("answer", correctAnswer);

            return parsedData;
        }
        return null;
    }
}