from stable_baselines3 import PPO
from stable_baselines3.common.env_util import make_vec_env
from MinecraftQuest import MinecraftQuestEnv
# Create the environment
def make_env():
    return MinecraftQuestEnv()

# Create the vectorized environment
env = make_vec_env(make_env, n_envs=4)

# Initialize the PPO model
model = PPO("MlpPolicy", env, verbose=1)

# Train the model
model.learn(total_timesteps=10000)

# Save the model
model.save("minecraft_quest_ppo")